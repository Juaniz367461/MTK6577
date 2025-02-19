package com.android.server.pm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.android.internal.util.FastXmlSerializer;


import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

final class VendorSettings {
    
    private static final String TAG_ROOT = "packages";
    private static final String TAG_PACKAGE = "package";
    static final String ATTR_PACKAGE_NAME = "name";
    static final String ATTR_INSTALL_STATUS = "installStatus";
    static final String VAL_INSTALLED = "installed";
    static final String VAL_UNINSTALLED = "uninstalled";
    
    private final File mSystemDir;
    private final File mVendorSettingsFilename;
    private final File mVendorBackupSettingsFilename;
    
    final HashMap<String, VendorPackageSetting> mVendorPackages =
            new HashMap<String, VendorPackageSetting>();
    VendorSettings() {
        this(Environment.getDataDirectory());
    }
    VendorSettings(File dataDir) {
        mSystemDir = new File(dataDir, "system");;
        mSystemDir.mkdirs();
        FileUtils.setPermissions(mSystemDir.toString(),
                FileUtils.S_IRWXU|FileUtils.S_IRWXG
                |FileUtils.S_IROTH|FileUtils.S_IXOTH,
                -1, -1);
        mVendorSettingsFilename = new File(mSystemDir, "vendor-packages.xml");
        mVendorBackupSettingsFilename = new File(mSystemDir, "vendor-packages-backup.xml");
    }

    void insertPackage(String packageName, boolean installStatus) {
        VendorPackageSetting vps = mVendorPackages.get(packageName);
        if (vps != null) {
            vps.setIntallStatus(installStatus);
        } else {
            vps = new VendorPackageSetting(packageName, installStatus);
            mVendorPackages.put(packageName, vps);
        }
    }

    void setPackageStatus(String packageName, boolean installStatus) {
        VendorPackageSetting vps = mVendorPackages.get(packageName);
        if (vps == null) {
            /// M: Shall we return a much meaningful result?
            return;
        } else {
            vps.setIntallStatus(installStatus);
        }
    }

    void removePackage(String packageName) {
        if (mVendorPackages.get(packageName) != null) {
            mVendorPackages.remove(packageName);    
        }
    }
    
    void readLPw() {
        FileInputStream str = null;
        DocumentBuilderFactory docBuilderFactory = null;
        DocumentBuilder docBuilder = null;
        Document doc = null;
        if (mVendorBackupSettingsFilename.exists()) {
            try {
                str = new FileInputStream(mVendorBackupSettingsFilename);
                if (mVendorSettingsFilename.exists()) {
                    /// M: If both the backup and vendor settings file exist, we
                    /// ignore the settings since it might have been corrupted.
                    Slog.w(PackageManagerService.TAG, "Cleaning up settings file");
                    mVendorSettingsFilename.delete();
                }
            } catch (java.io.IOException e) {
                
            }
        }
        
        try {
            if (str == null) {
                if (!mVendorSettingsFilename.exists()) {
                    return;
                }
                str = new FileInputStream(mVendorSettingsFilename);
            }
            docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(str);
            Element root = doc.getDocumentElement();
            NodeList nodeList = root.getElementsByTagName(TAG_PACKAGE);
            Node node = null;
            NamedNodeMap nodeMap = null;
            String packageName = null;
            String installStatus = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                node = nodeList.item(i);
                if (node.getNodeName().equals(TAG_PACKAGE)) {
                    nodeMap = node.getAttributes();
                    packageName = nodeMap.getNamedItem(ATTR_PACKAGE_NAME).getTextContent();
                    installStatus = nodeMap.getNamedItem(ATTR_INSTALL_STATUS).getTextContent();
                    mVendorPackages.put(packageName, 
                            new VendorPackageSetting(packageName, installStatus.equals(VAL_INSTALLED)));
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
    
    void writeLPr() {
        if (mVendorSettingsFilename.exists()) {
            if (!mVendorBackupSettingsFilename.exists()) {
                if (!mVendorSettingsFilename.renameTo(mVendorBackupSettingsFilename)) {
                    Slog.e(PackageManagerService.TAG, "Unable to backup package manager vendor settings, "
                            + " current changes will be lost at reboot");
                    return;
                }
            } else {
                mVendorSettingsFilename.delete();
                Slog.w(PackageManagerService.TAG, "Preserving older vendor settings backup");
            }
        }
        try {
            FileOutputStream fstr = new FileOutputStream(mVendorSettingsFilename);
            XmlSerializer serializer = new FastXmlSerializer();
            //XmlSerializer serializer = Xml.newSerializer()
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            serializer.setOutput(str, "utf-8");
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_ROOT);
            
            for (VendorPackageSetting ps : mVendorPackages.values()) {
                serializer.startTag(null, TAG_PACKAGE);
                serializer.attribute(null, ATTR_PACKAGE_NAME, ps.getPackageName());
                serializer.attribute(null, ATTR_INSTALL_STATUS, 
                        ps.getIntallStatus() ? VAL_INSTALLED : VAL_UNINSTALLED);
                serializer.endTag(null, TAG_PACKAGE);
            }
            serializer.endTag(null, TAG_ROOT);
            serializer.endDocument();
            str.flush();
            FileUtils.sync(fstr);
            str.close();  
            
            mVendorBackupSettingsFilename.delete();
            FileUtils.setPermissions(mVendorSettingsFilename.toString(),
                    FileUtils.S_IRUSR|FileUtils.S_IWUSR
                    |FileUtils.S_IRGRP|FileUtils.S_IWGRP,
                    -1, -1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
