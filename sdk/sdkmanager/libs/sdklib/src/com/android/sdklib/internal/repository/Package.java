/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.PkgProps;
import com.android.sdklib.repository.SdkAddonConstants;
import com.android.sdklib.repository.SdkRepoConstants;

import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link Package} is the base class for "something" that can be downloaded from
 * the SDK repository.
 * <p/>
 * A package has some attributes (revision, description) and a list of archives
 * which represent the downloadable bits.
 * <p/>
 * Packages are contained by a {@link SdkSource} (a download site).
 * <p/>
 * Derived classes must implement the {@link IDescription} methods.
 */
public abstract class Package implements IDescription, Comparable<Package> {

    private final int mRevision;
    private final String mObsolete;
    private final String mLicense;
    private final String mDescription;
    private final String mDescUrl;
    private final String mReleaseNote;
    private final String mReleaseUrl;
    private final Archive[] mArchives;
    private final SdkSource mSource;

    /**
     * Enum for the result of {@link Package#canBeUpdatedBy(Package)}. This used so that we can
     * differentiate between a package that is totally incompatible, and one that is the same item
     * but just not an update.
     * @see #canBeUpdatedBy(Package)
     */
    public static enum UpdateInfo {
        /** Means that the 2 packages are not the same thing */
        INCOMPATIBLE,
        /** Means that the 2 packages are the same thing but one does not upgrade the other.
         *  </p>
         *  TODO: this name is confusing. We need to dig deeper. */
        NOT_UPDATE,
        /** Means that the 2 packages are the same thing, and one is the upgrade of the other */
        UPDATE;
    }

    /**
     * Creates a new package from the attributes and elements of the given XML node.
     * This constructor should throw an exception if the package cannot be created.
     *
     * @param source The {@link SdkSource} where this is loaded from.
     * @param packageNode The XML element being parsed.
     * @param nsUri The namespace URI of the originating XML document, to be able to deal with
     *          parameters that vary according to the originating XML schema.
     * @param licenses The licenses loaded from the XML originating document.
     */
    Package(SdkSource source, Node packageNode, String nsUri, Map<String,String> licenses) {
        mSource = source;
        mRevision    = XmlParserUtils.getXmlInt   (packageNode, SdkRepoConstants.NODE_REVISION, 0);
        mDescription = XmlParserUtils.getXmlString(packageNode, SdkRepoConstants.NODE_DESCRIPTION);
        mDescUrl     = XmlParserUtils.getXmlString(packageNode, SdkRepoConstants.NODE_DESC_URL);
        mReleaseNote = XmlParserUtils.getXmlString(packageNode, SdkRepoConstants.NODE_RELEASE_NOTE);
        mReleaseUrl  = XmlParserUtils.getXmlString(packageNode, SdkRepoConstants.NODE_RELEASE_URL);
        mObsolete    = XmlParserUtils.getOptionalXmlString(
                                                   packageNode, SdkRepoConstants.NODE_OBSOLETE);

        mLicense  = parseLicense(packageNode, licenses);
        mArchives = parseArchives(XmlParserUtils.getFirstChild(
                                  packageNode, SdkRepoConstants.NODE_ARCHIVES));
    }

    /**
     * Manually create a new package with one archive and the given attributes.
     * This is used to create packages from local directories in which case there must be
     * one archive which URL is the actual target location.
     * <p/>
     * Properties from props are used first when possible, e.g. if props is non null.
     * <p/>
     * By design, this creates a package with one and only one archive.
     */
    public Package(
            SdkSource source,
            Properties props,
            int revision,
            String license,
            String description,
            String descUrl,
            Os archiveOs,
            Arch archiveArch,
            String archiveOsPath) {

        if (description == null) {
            description = "";
        }
        if (descUrl == null) {
            descUrl = "";
        }

        mRevision = Integer.parseInt(
                       getProperty(props, PkgProps.PKG_REVISION, Integer.toString(revision)));
        mLicense     = getProperty(props, PkgProps.PKG_LICENSE,      license);
        mDescription = getProperty(props, PkgProps.PKG_DESC,         description);
        mDescUrl     = getProperty(props, PkgProps.PKG_DESC_URL,     descUrl);
        mReleaseNote = getProperty(props, PkgProps.PKG_RELEASE_NOTE, "");
        mReleaseUrl  = getProperty(props, PkgProps.PKG_RELEASE_URL,  "");
        mObsolete    = getProperty(props, PkgProps.PKG_OBSOLETE,     null);

        // If source is null and we can find a source URL in the properties, generate
        // a dummy source just to store the URL. This allows us to easily remember where
        // a package comes from.
        String srcUrl = getProperty(props, PkgProps.PKG_SOURCE_URL, null);
        if (props != null && source == null && srcUrl != null) {
            // Both Addon and Extra packages can come from an addon source.
            // For Extras, we can tell by looking at the source URL.
            if (this instanceof AddonPackage ||
                    ((this instanceof ExtraPackage) &&
                     srcUrl.endsWith(SdkAddonConstants.URL_DEFAULT_FILENAME))) {
                source = new SdkAddonSource(srcUrl, null /*uiName*/);
            } else {
                source = new SdkRepoSource(srcUrl, null /*uiName*/);
            }
        }
        mSource = source;

        assert archiveOsPath != null;
        mArchives = initializeArchives(props, archiveOs, archiveArch, archiveOsPath);
    }

    /**
     * Called by the constructor to get the initial {@link #mArchives} array.
     * <p/>
     * This is invoked by the local-package constructor and allows mock testing
     * classes to override the archives created.
     * This is an <em>implementation</em> details and clients must <em>not</em>
     * rely on this.
     *
     * @return Always return a non-null array. The array may be empty.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    protected Archive[] initializeArchives(
            Properties props,
            Os archiveOs,
            Arch archiveArch,
            String archiveOsPath) {
        return new Archive[] {
                new Archive(this,
                    props,
                    archiveOs,
                    archiveArch,
                    archiveOsPath) };
    }

    /**
     * Utility method that returns a property from a {@link Properties} object.
     * Returns the default value if props is null or if the property is not defined.
     *
     * @param props The {@link Properties} to search into.
     *   If null, the default value is returned.
     * @param propKey The name of the property. Must not be null.
     * @param defaultValue The default value to return if {@code props} is null or if the
     *   key is not found. Can be null.
     * @return The string value of the given key in the properties, or null if the key
     *   isn't found or if {@code props} is null.
     */
    static String getProperty(Properties props, String propKey, String defaultValue) {
        if (props == null) {
            return defaultValue;
        }
        return props.getProperty(propKey, defaultValue);
    }

    /**
     * Save the properties of the current packages in the given {@link Properties} object.
     * These properties will later be give the constructor that takes a {@link Properties} object.
     */
    void saveProperties(Properties props) {
        props.setProperty(PkgProps.PKG_REVISION, Integer.toString(mRevision));
        if (mLicense != null && mLicense.length() > 0) {
            props.setProperty(PkgProps.PKG_LICENSE, mLicense);
        }

        if (mDescription != null && mDescription.length() > 0) {
            props.setProperty(PkgProps.PKG_DESC, mDescription);
        }
        if (mDescUrl != null && mDescUrl.length() > 0) {
            props.setProperty(PkgProps.PKG_DESC_URL, mDescUrl);
        }

        if (mReleaseNote != null && mReleaseNote.length() > 0) {
            props.setProperty(PkgProps.PKG_RELEASE_NOTE, mReleaseNote);
        }
        if (mReleaseUrl != null && mReleaseUrl.length() > 0) {
            props.setProperty(PkgProps.PKG_RELEASE_URL, mReleaseUrl);
        }
        if (mObsolete != null) {
            props.setProperty(PkgProps.PKG_OBSOLETE, mObsolete);
        }

        if (mSource != null) {
            props.setProperty(PkgProps.PKG_SOURCE_URL,  mSource.getUrl());
        }
    }

    /**
     * Parses the uses-licence node of this package, if any, and returns the license
     * definition if there's one. Returns null if there's no uses-license element or no
     * license of this name defined.
     */
    private String parseLicense(Node packageNode, Map<String, String> licenses) {
        Node usesLicense = XmlParserUtils.getFirstChild(
                                            packageNode, SdkRepoConstants.NODE_USES_LICENSE);
        if (usesLicense != null) {
            Node ref = usesLicense.getAttributes().getNamedItem(SdkRepoConstants.ATTR_REF);
            if (ref != null) {
                String licenseRef = ref.getNodeValue();
                return licenses.get(licenseRef);
            }
        }
        return null;
    }

    /**
     * Parses an XML node to process the <archives> element.
     * Always return a non-null array. The array may be empty.
     */
    private Archive[] parseArchives(Node archivesNode) {
        ArrayList<Archive> archives = new ArrayList<Archive>();

        if (archivesNode != null) {
            String nsUri = archivesNode.getNamespaceURI();
            for(Node child = archivesNode.getFirstChild();
                     child != null;
                     child = child.getNextSibling()) {

                if (child.getNodeType() == Node.ELEMENT_NODE &&
                        nsUri.equals(child.getNamespaceURI()) &&
                        SdkRepoConstants.NODE_ARCHIVE.equals(child.getLocalName())) {
                    archives.add(parseArchive(child));
                }
            }
        }

        return archives.toArray(new Archive[archives.size()]);
    }

    /**
     * Parses one <archive> element from an <archives> container.
     */
    private Archive parseArchive(Node archiveNode) {
        Archive a = new Archive(
                    this,
                    (Os)   XmlParserUtils.getEnumAttribute(archiveNode, SdkRepoConstants.ATTR_OS,
                            Os.values(), null),
                    (Arch) XmlParserUtils.getEnumAttribute(archiveNode, SdkRepoConstants.ATTR_ARCH,
                            Arch.values(), Arch.ANY),
                    XmlParserUtils.getXmlString(archiveNode, SdkRepoConstants.NODE_URL),
                    XmlParserUtils.getXmlLong  (archiveNode, SdkRepoConstants.NODE_SIZE, 0),
                    XmlParserUtils.getXmlString(archiveNode, SdkRepoConstants.NODE_CHECKSUM)
                );

        return a;
    }

    /**
     * Returns the source that created (and owns) this package. Can be null.
     */
    public SdkSource getParentSource() {
        return mSource;
    }

    /**
     * Returns true if the package is deemed obsolete, that is it contains an
     * actual <code>&lt;obsolete&gt;</code> element.
     */
    public boolean isObsolete() {
        return mObsolete != null;
    }

    /**
     * Returns the revision, an int > 0, for all packages (platform, add-on, tool, doc).
     * Can be 0 if this is a local package of unknown revision.
     */
    public int getRevision() {
        return mRevision;
    }

    /**
     * Returns the optional description for all packages (platform, add-on, tool, doc) or
     * for a lib. It is null if the element has not been specified in the repository XML.
     */
    public String getLicense() {
        return mLicense;
    }

    /**
     * Returns the optional description for all packages (platform, add-on, tool, doc) or
     * for a lib. Can be empty but not null.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the optional description URL for all packages (platform, add-on, tool, doc).
     * Can be empty but not null.
     */
    public String getDescUrl() {
        return mDescUrl;
    }

    /**
     * Returns the optional release note for all packages (platform, add-on, tool, doc) or
     * for a lib. Can be empty but not null.
     */
    public String getReleaseNote() {
        return mReleaseNote;
    }

    /**
     * Returns the optional release note URL for all packages (platform, add-on, tool, doc).
     * Can be empty but not null.
     */
    public String getReleaseNoteUrl() {
        return mReleaseUrl;
    }

    /**
     * Returns the archives defined in this package.
     * Can be an empty array but not null.
     */
    public Archive[] getArchives() {
        return mArchives;
    }

    /**
     * Returns true if this package contains the exact given archive.
     * Important: This compares object references, not object equality.
     */
    public boolean hasArchive(Archive archive) {
        for (Archive a : mArchives) {
            if (a == archive) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the {@link Package} has at least one {@link Archive} compatible with
     * the host platform.
     */
    public boolean hasCompatibleArchive() {
        for (Archive archive : mArchives) {
            if (archive.isCompatible()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a short, reasonably unique string identifier that can be used
     * to identify this package when installing from the command-line interface.
     * {@code 'android list sdk'} will show these IDs and then in turn they can
     * be provided to {@code 'android update sdk --no-ui --filter'} to select
     * some specific packages.
     * <p/>
     * The identifiers must have the following properties: <br/>
     * - They must contain only simple alphanumeric characters. <br/>
     * - Commas, whitespace and any special character that could be obviously problematic
     *   to a shell interface should be avoided (so dash/underscore are OK, but things
     *   like colon, pipe or dollar should be avoided.) <br/>
     * - The name must be consistent across calls and reasonably unique for the package
     *   type. Collisions can occur but should be rare. <br/>
     * - Different package types should have a clearly different name pattern. <br/>
     * - The revision number should not be included, as this would prevent updates
     *   from being automated (which is the whole point.) <br/>
     * - It must remain reasonably human readable. <br/>
     * - If no such id can exist (for example for a local package that cannot be installed)
     *   then an empty string should be returned. Don't return null.
     * <p/>
     * Important: This is <em>not</em> a strong unique identifier for the package.
     * If you need a strong unique identifier, you should use {@link #comparisonKey()}
     * and the {@link Comparable} interface.
     */
    public abstract String installId();

    /**
     * Returns the short description of the source, if not null.
     * Otherwise returns the default Object toString result.
     * <p/>
     * This is mostly helpful for debugging.
     * For UI display, use the {@link IDescription} interface.
     */
    @Override
    public String toString() {
        String s = getShortDescription();
        if (s != null) {
            return s;
        }
        return super.toString();
    }

    /**
     * Returns a description of this package that is suitable for a list display.
     * Should not be empty. Must never be null.
     * <p/>
     * Note that this is the "base" name for the package
     * with no specific revision nor API mentionned.
     * In contrast, {@link #getShortDescription()} should be used if you want more details
     * such as the package revision number or the API, if applicable.
     */
    public abstract String getListDescription();

    /**
     * Returns a short description for an {@link IDescription}.
     * Can be empty but not null.
     */
    public abstract String getShortDescription();

    /**
     * Returns a long description for an {@link IDescription}.
     * Can be empty but not null.
     */
    public String getLongDescription() {
        StringBuilder sb = new StringBuilder();

        String s = getDescription();
        if (s != null) {
            sb.append(s);
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }

        sb.append(String.format("Revision %1$d%2$s",
                getRevision(),
                isObsolete() ? " (Obsolete)" : ""));

        s = getDescUrl();
        if (s != null && s.length() > 0) {
            sb.append(String.format("\n\nMore information at %1$s", s));
        }

        s = getReleaseNote();
        if (s != null && s.length() > 0) {
            sb.append("\n\nRelease note:\n").append(s);
        }

        s = getReleaseNoteUrl();
        if (s != null && s.length() > 0) {
            sb.append("\nRelease note URL: ").append(s);
        }

        return sb.toString();
    }

    /**
     * A package is local (that is 'installed locally') if it contains a single
     * archive that is local. If not local, it's a remote package, only available
     * on a remote source for download and installation.
     */
    public boolean isLocal() {
        return mArchives.length == 1 && mArchives[0].isLocal();
    }

    /**
     * Computes a potential installation folder if an archive of this package were
     * to be installed right away in the given SDK root.
     * <p/>
     * Some types of packages install in a fix location, for example docs and tools.
     * In this case the returned folder may already exist with a different archive installed
     * at the desired location. <br/>
     * For other packages types, such as add-on or platform, the folder name is only partially
     * relevant to determine the content and thus a real check will be done to provide an
     * existing or new folder depending on the current content of the SDK.
     * <p/>
     * Note that the installer *will* create all directories returned here just before
     * installation so this method must not attempt to create them.
     *
     * @param osSdkRoot The OS path of the SDK root folder.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    public abstract File getInstallFolder(String osSdkRoot, SdkManager sdkManager);

    /**
     * Hook called right before an archive is installed. The archive has already
     * been downloaded successfully and will be installed in the directory specified by
     * <var>installFolder</var> when this call returns.
     * <p/>
     * The hook lets the package decide if installation of this specific archive should
     * be continue. The installer will still install the remaining packages if possible.
     * <p/>
     * The base implementation always return true.
     * <p/>
     * Note that the installer *will* create all directories specified by
     * {@link #getInstallFolder} just before installation, so they must not be
     * created here. This is also called before the previous install dir is removed
     * so the previous content is still there during upgrade.
     *
     * @param archive The archive that will be installed
     * @param monitor The {@link ITaskMonitor} to display errors.
     * @param osSdkRoot The OS path of the SDK root folder.
     * @param installFolder The folder where the archive will be installed. Note that this
     *                      is <em>not</em> the folder where the archive was temporary
     *                      unzipped. The installFolder, if it exists, contains the old
     *                      archive that will soon be replaced by the new one.
     * @return True if installing this archive shall continue, false if it should be skipped.
     */
    public boolean preInstallHook(Archive archive, ITaskMonitor monitor,
            String osSdkRoot, File installFolder) {
        // Nothing to do in base class.
        return true;
    }

    /**
     * Hook called right after an archive has been installed.
     *
     * @param archive The archive that has been installed.
     * @param monitor The {@link ITaskMonitor} to display errors.
     * @param installFolder The folder where the archive was successfully installed.
     *                      Null if the installation failed, in case the archive needs to
     *                      do some cleanup after <code>preInstallHook</code>.
     */
    public void postInstallHook(Archive archive, ITaskMonitor monitor, File installFolder) {
        // Nothing to do in base class.
    }

    /**
     * Returns whether the give package represents the same item as the current package.
     * <p/>
     * Two packages are considered the same if they represent the same thing, except for the
     * revision number.
     * @param pkg the package to compare
     * @return true if the item
     */
    public abstract boolean sameItemAs(Package pkg);

    /**
     * Computes whether the given package is a suitable update for the current package.
     * <p/>
     * An update is just that: a new package that supersedes the current one. If the new
     * package does not represent the same item or if it has the same or lower revision as the
     * current one, it's not an update.
     *
     * @param replacementPackage The potential replacement package.
     * @return One of the {@link UpdateInfo} values.
     *
     * @see #sameItemAs(Package)
     */
    public UpdateInfo canBeUpdatedBy(Package replacementPackage) {
        if (replacementPackage == null) {
            return UpdateInfo.INCOMPATIBLE;
        }

        // check they are the same item.
        if (sameItemAs(replacementPackage) == false) {
            return UpdateInfo.INCOMPATIBLE;
        }

        // check revision number
        if (replacementPackage.getRevision() > this.getRevision()) {
            return UpdateInfo.UPDATE;
        }

        // not an upgrade but not incompatible either.
        return UpdateInfo.NOT_UPDATE;
    }

    /**
     * Returns an ordering like this: <br/>
     * - Tools <br/>
     * - Platform-Tools <br/>
     * - Docs. <br/>
     * - Platform n preview <br/>
     * - Platform n <br/>
     * - Platform n-1 <br/>
     * - Samples packages <br/>
     * - Add-on based on n preview <br/>
     * - Add-on based on n <br/>
     * - Add-on based on n-1 <br/>
     * - Extra packages <br/>
     * <p/>
     * Important: this must NOT be used to compare if two packages are the same thing.
     * This is achieved by {@link #sameItemAs(Package)} or {@link #canBeUpdatedBy(Package)}.
     * <p/>
     * This {@link #compareTo(Package)} method is purely an implementation detail to
     * perform the right ordering of the packages in the list of available or installed packages.
     * <p/>
     * <em>Important</em>: Derived classes should consider overriding {@link #comparisonKey()}
     * instead of this method.
     */
    public int compareTo(Package other) {
        String s1 = this.comparisonKey();
        String s2 = other.comparisonKey();

        return s1.compareTo(s2);
    }

    /**
     * Computes a comparison key for each package used by {@link #compareTo(Package)}.
     * The key is a string.
     * The base package class return a string that encodes the package type,
     * the revision number and the platform version, if applicable, in the form:
     * <pre>
     *      t:N|v:NNNN.P|r:NNNN|
     * </pre>
     * All fields must start by a "letter colon" prefix and end with a vertical pipe (|, ASCII 124).
     * <p/>
     * The string format <em>may</em> change between releases and clients should not
     * store them outside of the session or expect them to be consistent between
     * different releases. They are purely an internal implementation details of the
     * {@link #compareTo(Package)} method.
     * <p/>
     * Derived classes should get the string from the super class and then append
     * or <em>insert</em> their own |-separated content.
     * For example an extra vendor name & path can be inserted before the revision
     * number, since it has more sorting weight.
     */
    protected String comparisonKey() {

        StringBuilder sb = new StringBuilder();

        sb.append("t:");                                                        //$NON-NLS-1$
        if (this instanceof ToolPackage) {
            sb.append(0);
        } else if (this instanceof PlatformToolPackage) {
            sb.append(1);
        } else if (this instanceof DocPackage) {
            sb.append(2);
        } else if (this instanceof PlatformPackage) {
            sb.append(3);
        } else if (this instanceof SamplePackage) {
            sb.append(4);
        } else if (this instanceof SystemImagePackage) {
            sb.append(5);
        } else if (this instanceof AddonPackage) {
            sb.append(6);
        } else {
            // extras and everything else
            sb.append(9);
        }
        sb.append("|v:");                                                       //$NON-NLS-1$


        // We insert the package version here because it is more important
        // than the revision number. We want package version to be sorted
        // top-down, so we'll use 10k-api as the sorting key. The day we
        // get reach 10k APIs, we'll need to revisit this.

        if (this instanceof IPackageVersion) {
            AndroidVersion v = ((IPackageVersion) this).getVersion();

            sb.append(String.format("%1$04d.%2$d",                              //$NON-NLS-1$
                    10000 - v.getApiLevel(),
                    v.isPreview() ? 1 : 0
                    ));
        }
        sb.append("|r:");                                                       //$NON-NLS-1$


        // Append revision number

        sb.append(String.format("%1$04d", getRevision()));                      //$NON-NLS-1$
        sb.append('|');

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(mArchives);
        result = prime * result + ((mObsolete == null) ? 0 : mObsolete.hashCode());
        result = prime * result + mRevision;
        result = prime * result + ((mSource == null) ? 0 : mSource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Package)) {
            return false;
        }
        Package other = (Package) obj;
        if (!Arrays.equals(mArchives, other.mArchives)) {
            return false;
        }
        if (mObsolete == null) {
            if (other.mObsolete != null) {
                return false;
            }
        } else if (!mObsolete.equals(other.mObsolete)) {
            return false;
        }
        if (mRevision != other.mRevision) {
            return false;
        }
        if (mSource == null) {
            if (other.mSource != null) {
                return false;
            }
        } else if (!mSource.equals(other.mSource)) {
            return false;
        }
        return true;
    }
}
