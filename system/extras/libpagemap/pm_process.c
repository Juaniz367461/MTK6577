/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <pagemap/pagemap.h>

#include "pm_map.h"

static int read_maps(pm_process_t *proc);

#define MAX_FILENAME 64

int pm_process_create(pm_kernel_t *ker, pid_t pid, pm_process_t **proc_out) {
    pm_process_t *proc;
    char filename[MAX_FILENAME];
    int error;

    if (!ker || !proc_out)
        return -1;

    proc = calloc(1, sizeof(*proc));
    if (!proc)
        return errno;

    proc->ker = ker;
    proc->pid = pid;

    error = snprintf(filename, MAX_FILENAME, "/proc/%d/pagemap", pid);
    if (error < 0 || error >= MAX_FILENAME) {
        error = (error < 0) ? (errno) : (-1);
        free(proc);
        return error;
    }

    proc->pagemap_fd = open(filename, O_RDONLY);
    if (proc->pagemap_fd < 0) {
        error = errno;
        free(proc);
        return error;
    }        

    error = read_maps(proc);
    if (error) {
        free(proc);
        return error;
    }

    *proc_out = proc;

    return 0;
}

int pm_process_usage(pm_process_t *proc, pm_memusage_t *usage_out) {
    pm_memusage_t usage, map_usage;
    int error;
    int i;

    if (!proc || !usage_out)
        return -1;

    pm_memusage_zero(&usage);

    for (i = 0; i < proc->num_maps; i++) {
        error = pm_map_usage(proc->maps[i], &map_usage);
        if (error) return error;

        pm_memusage_add(&usage, &map_usage);
    }

    memcpy(usage_out, &usage, sizeof(pm_memusage_t));

    return 0;
}

int pm_process_pagemap_range(pm_process_t *proc,
                             unsigned long low, unsigned long high,
                             uint64_t **range_out, size_t *len) {
    int firstpage, numpages;
    uint64_t *range;
    off_t off;
    int error;

    if (!proc || (low >= high) || !range_out || !len)
        return -1;

    firstpage = low / proc->ker->pagesize;
    numpages = (high - low) / proc->ker->pagesize;

    range = malloc(numpages * sizeof(uint64_t));
    if (!range)
        return errno;

    off = lseek(proc->pagemap_fd, firstpage * sizeof(uint64_t), SEEK_SET);
    if (off == (off_t)-1) {
        error = errno;
        free(range);
        return error;
    }
    error = read(proc->pagemap_fd, (char*)range, numpages * sizeof(uint64_t));
    if (error == 0) {
        /* EOF, mapping is not in userspace mapping range (probably vectors) */
        *len = 0;
        free(range);
        *range_out = NULL;
        return 0;
    } else if (error < 0 || (error > 0 && error < numpages * sizeof(uint64_t))) {
        error = (error < 0) ? errno : -1;
        free(range);
        return error;
    }

    *range_out = range;
    *len = numpages;

    return 0;
}

int pm_process_maps(pm_process_t *proc, pm_map_t ***maps_out, size_t *len) {
    pm_map_t **maps;

    if (!proc || !maps_out || !len)
        return -1;

    if (proc->num_maps) {
        maps = malloc(proc->num_maps * sizeof(pm_map_t*));
        if (!maps)
            return errno;

        memcpy(maps, proc->maps, proc->num_maps * sizeof(pm_map_t*));
    
        *maps_out = maps;
    } else {
        *maps_out = NULL;
    }
    *len = proc->num_maps;

    return 0;
}

int pm_process_workingset(pm_process_t *proc,
                          pm_memusage_t *ws_out, int reset) {
    pm_memusage_t ws, map_ws;
    char filename[MAX_FILENAME];
    int fd;
    int i, j;
    int error;

    if (!proc)
        return -1;

    if (ws_out) {
        pm_memusage_zero(&ws);
        for (i = 0; i < proc->num_maps; i++) {
            error = pm_map_workingset(proc->maps[i], &map_ws);
            if (error) return error;

            pm_memusage_add(&ws, &map_ws);
        }
        
        memcpy(ws_out, &ws, sizeof(ws));
    }

    if (reset) {
        error = snprintf(filename, MAX_FILENAME, "/proc/%d/clear_refs",
                         proc->pid);
        if (error < 0 || error >= MAX_FILENAME) {
            return (error < 0) ? (errno) : (-1);
        }

        fd = open(filename, O_WRONLY);
        if (fd < 0)
            return errno;

        write(fd, "1\n", strlen("1\n"));

        close(fd);
    }

    return 0;
}

int pm_process_destroy(pm_process_t *proc) {
    if (!proc)
        return -1;

    free(proc->maps);
    close(proc->pagemap_fd);
    free(proc);

    return 0;
}

#define INITIAL_MAPS 10
#define MAX_LINE  512  //LiChunhui modify for brower issue.
#define MAX_PERMS 5

/* 
 * #define FOO 123
 * S(FOO) => "123"
 */
#define _S(n) #n
#define S(n) _S(n)

static int read_maps(pm_process_t *proc) {
    char filename[MAX_FILENAME];
    char line[MAX_LINE], name[MAX_LINE], perms[MAX_PERMS];
    FILE *maps_f;
    pm_map_t *map, **maps, **new_maps;
    int maps_count, maps_size;
    int error;
       
    if (!proc)
        return -1;

    maps = calloc(INITIAL_MAPS, sizeof(pm_map_t*));
    if (!maps)
        return errno;
    maps_count = 0; maps_size = INITIAL_MAPS;

    error = snprintf(filename, MAX_FILENAME, "/proc/%d/maps", proc->pid);
    if (error < 0 || error >= MAX_FILENAME)
        return (error < 0) ? (errno) : (-1);

    maps_f = fopen(filename, "r");
    if (!maps_f)
        return errno;

    while (fgets(line, MAX_LINE, maps_f)) {
        if (maps_count >= maps_size) {
            new_maps = realloc(maps, 2 * maps_size * sizeof(pm_map_t*));
            if (!new_maps) {
                error = errno;
                free(maps);
                fclose(maps_f);
                return error;
            }
            maps = new_maps;
            maps_size *= 2;
        }

        maps[maps_count] = map = calloc(1, sizeof(*map));

        map->proc = proc;

        sscanf(line, "%lx-%lx %s %lx %*s %*d %" S(MAX_LINE) "s",
               &map->start, &map->end, perms, &map->offset, name);

        map->name = malloc(strlen(name) + 1);
        if (!map->name) {
            error = errno;
            for (; maps_count > 0; maps_count--)
                pm_map_destroy(maps[maps_count]);
            free(maps);
            return error;
        }
        strcpy(map->name, name);
        if (perms[0] == 'r') map->flags |= PM_MAP_READ;
        if (perms[1] == 'w') map->flags |= PM_MAP_WRITE;
        if (perms[2] == 'x') map->flags |= PM_MAP_EXEC;

        maps_count++;
    }

    fclose(maps_f);

    new_maps = realloc(maps, maps_count * sizeof(pm_map_t*));
    if (maps_count && !new_maps) {
        error = errno;
        free(maps);
        return error;
    }

    proc->maps = new_maps;
    proc->num_maps = maps_count;

    return 0;
}
