#define LOG_TAG "ThemeManager"
//#define LOG_NDEBUG 0	/* Use to control whether LOGV is shown. */

#include <utils/MTKThemeManager.h>
#include <utils/Log.h>
#include <utils/String8.h>
#include "tinyxml.h"

using namespace android;

static const char* kThemeMapFile = "/system/etc/theme/thememap.xml";
static const char* kThemeModuleName = "Module";
static const char* kThemePathAttr = "path";
static const char* kThemeItemName = "item";

MTKThemeManager* MTKThemeManager::mInstance = NULL;
int MTKThemeManager::mModuleCount = 0;
int getChildNodeCount(const char* childName, TiXmlNode *parentNode, TiXmlNode *firstNode);

MTKThemeManager::MTKThemeManager()
{
	LOGV("MTKThemeManager constructor mInstance = %d.", mInstance);
	mPathMap = NULL;
}

MTKThemeManager::~MTKThemeManager()
{
	LOGV("MTKThemeManager deconstructor mInstance = %d.", mInstance);
	/* Recycle theme file list arrays. */
	for (int i = 0; i < MTKThemeManager::mModuleCount; i++) {
		if (mPathMap[i].fileList != NULL) {
			delete[] mPathMap[i].fileList;
			mPathMap[i].fileList = NULL;
		}
	}

	/* Recycle theme map array. */
	if (mPathMap != NULL)
	{
		delete[] mPathMap;
		mPathMap = NULL;
	}
}

/**
 * Return a MTKThemeManager instance, if there is an exists one,
 * use it, otherwise, construct an object, using singleton mode.
 */
MTKThemeManager* MTKThemeManager::getInstance()
{
	LOGV("MTKThemeManager getInstance start mInstance = %d.", mInstance);
	if (mInstance == NULL)
	{
		mInstance = new MTKThemeManager();
	}
	LOGV("MTKThemeManager getInstance end mInstance = %d.", mInstance);
	return mInstance;
}

void MTKThemeManager::releaseInstance() {
	LOGV("MTKThemeManager releaseInstance mInstance = %d.", mInstance);
	if (mInstance != NULL)
	{
		delete mInstance;
		mInstance = NULL;
	}
}

/**
 * Judge whether the given resource of the apk is in the theme resource map,
 * first find the apkName in the mPathMap array, if find successfully, then find
 * the fileName in the file list of the found apk, return true if the resource
 * of the apk is the in map, else false.
 */
bool MTKThemeManager::isFileInMap(const char* apkName, const char* fileName)
{
    int i;
    int moduleCnt = MTKThemeManager::mModuleCount;
    bool ret = false;

	LOGV("isFileInMap start apkName = %s, fileName = %s, moduleCnt = %d. \n", apkName, fileName, moduleCnt);
	if (moduleCnt == 0)	/* There is no apks in the map.*/
	{
		LOGI("isFileInMap return false because no apk path in path map list.");
		return false;
	}

	/* Find apk in the mPathMap, record the index if find successfully. */
	for (i = 0; i < moduleCnt; i++)
	{
//		LOGV("isFileInMap i = %d, mPathMap[i].path = %s.", i, mPathMap[i].path);
		if (strstr(apkName, mPathMap[i].path))
		{
			LOGV("isMappedFile i = %d, mPathMap[i].path = %s, listLen = %d, file = %s.\n",
					i, mPathMap[i].path, mPathMap[i].fileCount, fileName);
			break;
		}
	}

	if (i == moduleCnt || mPathMap[i].fileList == NULL || mPathMap[i].fileCount == 0)
	{
		LOGV("There is no such apk path in the theme map, or there is no items in the apk file list.\n");
		return false;
	}

	ret = findFileInList(mPathMap[i].fileList, mPathMap[i].fileCount, fileName);
	LOGV("isFileInMap end apkName = %s, fileName = %s,ret = %d.\n", apkName, fileName, ret);

	return ret;
}

/**
 * Find the specified fileName in the given fileList, if find successfully, return true, else false.
 */
bool MTKThemeManager::findFileInList(ThemeFileList *fileList, int listLen, const char* fileName) {
//	LOGV("Run into findFileInList listLen = %d, fileName = %s.\n", listLen, fileName);
	for (int i = 0; i < listLen; i++)
	{
//		LOGV("findFileInList i=%d, fileList[i].fileName = %s,fileName = %s.\n", i, fileList[i].fileName, fileName);
		if (fileList[i].fileName != NULL && strstr(fileName, fileList[i].fileName))
		{
//			LOGV("findFileInList return true,fileName = %s.\n", fileName);
			return true;
		}
	}
	return false;
}

/**
 * Parse the theme map if the mPathMap is empty, else use the mPathMap parsed before.
 */
void MTKThemeManager::parseThemeMapIfNeeded()
{
	if (mPathMap != NULL) {
		LOGV("The path has already parsed.");
		return;
	}

	/* Load theme map xml file. */
	TiXmlDocument* pDoc = new TiXmlDocument(kThemeMapFile);
	if (pDoc == NULL)
	{
		LOGE("Read theme map xml file failed!");
		return;
	}
	pDoc->LoadFile();

	/* Get the root node(thememap) and the first module child node.*/
	TiXmlElement *pRootElement = pDoc->RootElement();
	TiXmlElement *pFirstModuleElement = pRootElement->FirstChildElement(kThemeModuleName);
	LOGV("Module element is %s, path = %s.", pFirstModuleElement->Value(), pFirstModuleElement->Attribute(kThemePathAttr));

	/* Get module node count to create the path map array.*/
	int moduleCnt = getChildNodeCount(kThemeModuleName, pRootElement, pFirstModuleElement);
	LOGV("Total element count is %d.", moduleCnt);

	mPathMap = new ThemePathMap[moduleCnt];
	if (mPathMap == NULL)
	{
		LOGE("Failed to allocate memory for theme path map.");
		return;
	}
	MTKThemeManager::mModuleCount = moduleCnt;

	TiXmlNode *pModuleNode = pFirstModuleElement;
	TiXmlNode *pItemNode = NULL;
	TiXmlNode *pFirstItemElement = NULL;
	int itemCnt = 0;
	int moduleIndex = 0;
	int tempIndex = 0;

	/* Parse the whole xml by module. */
	while (pModuleNode != NULL)
	{
		mPathMap[moduleIndex].path = ((TiXmlElement *)pModuleNode)->Attribute(kThemePathAttr);
		LOGV("parseThemeMap while start moduleIndex = %d, pModuleNode = %d, path = %s.",
				moduleIndex, pModuleNode, mPathMap[moduleIndex].path);

		pFirstItemElement = pModuleNode->FirstChildElement(kThemeItemName);
		itemCnt = getChildNodeCount(kThemeItemName, pModuleNode, pFirstItemElement);
		mPathMap[moduleIndex].fileCount = itemCnt;
		if (itemCnt == 0)
		{
			LOGD("There is no item in apk %s.", ((TiXmlElement *)pModuleNode)->Attribute(kThemePathAttr));
			mPathMap[moduleIndex].fileList = NULL;
			continue;
		}

		ThemeFileList *itemFileList = new ThemeFileList[itemCnt];
		if (itemFileList == NULL)
		{
			LOGE("Failed to allocate memory for item file list array.");
			return;
		}

		pItemNode = pFirstItemElement;
		tempIndex = 0;
		/* Parse all items in the current module pModuleNode. */
		while (pItemNode != NULL)
		{
			itemFileList[tempIndex++].fileName = ((TiXmlElement *)pItemNode)->GetText();
			LOGV("parseThemeMap pItemNode->GetText() = %s, itemFileList[tempIndex].fileName = %s.",
					((TiXmlElement *)pItemNode)->GetText(), itemFileList[tempIndex-1].fileName);
			pItemNode = (TiXmlElement *)pModuleNode->IterateChildren(kThemeItemName, pItemNode);
		}

		mPathMap[moduleIndex].fileList = itemFileList;
		LOGV("parseThemeMap moduleIndex = %d, itemCnt = %d, mPathMap[moduleIndex].fileList = %d,"
						"itemFileList = %d, filename0 = %s, itemFileList filename0 = %s.",
						moduleIndex, itemCnt,
						mPathMap[moduleIndex].fileList, itemFileList,
						(mPathMap[moduleIndex].fileList)[0].fileName, itemFileList[0].fileName);
		moduleIndex++;

		pModuleNode = (TiXmlElement *)pRootElement->IterateChildren(kThemeModuleName, pModuleNode);
	}
	LOGV("Theme path map parsed completely.");
}

/** This function can not be set as member function because the include file issue. */
int getChildNodeCount(const char* childName, TiXmlNode *parentNode, TiXmlNode *firstNode)
{
	int nodeCount = 0;
	TiXmlNode *pNode = firstNode;
	while (pNode != NULL)
	{
		nodeCount++;
		pNode = parentNode->IterateChildren(childName, pNode);
	}
	return nodeCount;
}

/**
 * Helper class used for debug.
 */
void MTKThemeManager::dumpMapInfo()
{
	int i, j = 0;
	LOGV("dumpMapInfo: moduleCount = %d,module1 = %s,module2 = %s.", MTKThemeManager::mModuleCount,
			mPathMap[0].path, mPathMap[1].path);
	for (i = 0; i < MTKThemeManager::mModuleCount; i++ )
	{
		LOGV("dumpMapInfo: i = %d, path = %s, item = %s.", i, mPathMap[i].path,
				(mPathMap[i].fileList[0]).fileName);
	}
}


