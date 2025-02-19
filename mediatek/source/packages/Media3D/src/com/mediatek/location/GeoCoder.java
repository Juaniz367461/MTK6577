package com.mediatek.location;

import android.location.Location;
import com.mediatek.media3d.LogUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

public class GeoCoder {
    private static final String YAHOO_GEOCODE_API_URL =
        "http://where.yahooapis.com/geocode?" + "q=%1$s,%2$s&gflags=R&appid=%3$s";

    private static final String YAHOO_PLACE_BELONGS_API_URL =
        "http://where.yahooapis.com/v1/place/%1$s/belongtos?&appid=%2$s";

    private static final String APP_ID =
        "oSR281PV34HtuofJ71Fkjk3p1W3S7ia_D" + "q.Sw4Vfqir_6t5JKRc_UL2qZ2aynQ_7jnGx";

    public String getGeoCodeQueryUrl(Location location) {
        return String.format(YAHOO_GEOCODE_API_URL, String.valueOf(location.getLatitude()),
            String.valueOf(location.getLongitude()), APP_ID);
    }

    public String getBelongsQueryUrl(final long woeid) {
        return String.format(YAHOO_PLACE_BELONGS_API_URL, String.valueOf(woeid), APP_ID);
    }

    public String getHttpResponse(String url) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse getResponse = client.execute(get);
            HttpEntity getResponseEntity = getResponse.getEntity();
            if (null != getResponseEntity) {
                return EntityUtils.toString(getResponseEntity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LocationEx getCityFromGeoCode(Location location) {
        String geoCodeQueryUrl = getGeoCodeQueryUrl(location);
        LogUtil.v("GeoCoder", "url : " + geoCodeQueryUrl);
        String geoCodeResponse = getHttpResponse(geoCodeQueryUrl);
        LocationEx locationEx = parseGeoResponse(geoCodeResponse, location);
        if (locationEx.isTown()) {
            return locationEx;
        }

        // Find "town" level location.
        String queryBelongsUrl = getBelongsQueryUrl(locationEx.getWoeid());
        LogUtil.v("GeoCoder", "belongs url : " + queryBelongsUrl);
        String belongsResponse = getHttpResponse(queryBelongsUrl);
        return parseGeoResponse2(belongsResponse, location);
    }

    public LocationEx parseGeoResponse(String response, Location location) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(response)));
            NodeList resultNodes = doc.getElementsByTagName("Result");
            Node resultNode = resultNodes.item(0);
            NodeList attributesNodeList = resultNode.getChildNodes();

            LocationEx locationEx = new LocationEx(location);
            for (int i = 0; i < attributesNodeList.getLength(); i++) {
                Node node = attributesNodeList.item(i);
                Node firstChild = node.getFirstChild();

                if (firstChild == null) {
                    continue;
                }

                if ("latitude".equalsIgnoreCase(node.getNodeName())) {
                    locationEx.setLatitude(Double.parseDouble(firstChild
                        .getNodeValue()));
                }
                if ("longitude".equalsIgnoreCase(node.getNodeName())) {
                    locationEx.setLongitude(Double.parseDouble(firstChild
                        .getNodeValue()));
                }
                if ("city".equalsIgnoreCase(node.getNodeName())) {
                    locationEx.setCity(firstChild.getNodeValue());
                }
                if ("woeid".equalsIgnoreCase(node.getNodeName())) {
                    locationEx.setWoeid(Long.parseLong(firstChild.getNodeValue()));
                }
                if ("woetype".equalsIgnoreCase(node.getNodeName())) {
                    int woeType = Integer.parseInt(firstChild.getNodeValue());
                    LogUtil.v("GeoCoder", "Code :" + woeType);
                    if (woeType == 7) { // TOWN
                        locationEx.setAsTown(true);
                    }
                }
            }
            return locationEx;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LocationEx parseGeoResponse2(String response, Location location) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(response)));

            NodeList resultNodes = doc.getElementsByTagName("place");

            // Find places
            for (int i = 0; i < resultNodes.getLength(); ++i ) {
                NodeList attributes = resultNodes.item(i).getChildNodes();
                LocationEx locationExAsTown = null;
                long woeid = -1;

                // For each place
                for (int j = 0; j < attributes.getLength(); ++j) {
                    Node attribute = attributes.item(j);
                    Node first = attribute.getFirstChild();
                    if (first == null) {
                        continue;
                    }

                    if ("woeid".equalsIgnoreCase(attribute.getNodeName())) {
                        woeid = Long.parseLong(first.getNodeValue());
                    }

                    if ("placeTypeName".equalsIgnoreCase(attribute.getNodeName())) {
                        Node code = attribute.getAttributes().getNamedItem("code");
                        int codeValue = Integer.parseInt(code.getNodeValue());
                        if (codeValue == 7) {
                            locationExAsTown = new LocationEx(location);
                            locationExAsTown.setWoeid(woeid);
                        } else {
                            continue;
                        }
                    }

                    if ("name".equalsIgnoreCase(attribute.getNodeName())) {
                        if (locationExAsTown != null) {
                            locationExAsTown.setCity(first.getNodeValue());
                        }
                    }
                }

                if (locationExAsTown != null) {
                    LogUtil.v("GeoCoder", "Name: " + locationExAsTown.getCity() +
                        ", woeid :" + locationExAsTown.getWoeid());
                    return locationExAsTown;
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}