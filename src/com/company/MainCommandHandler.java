package com.company;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.w3c.dom.*;
import org.w3c.dom.stylesheets.StyleSheet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

public class MainCommandHandler {
    private Scanner mScanner;
    private static String workingDirectory;
    private FileHandler mFileHandler;
    private static int targetRow = -1;
    private static boolean hasCsv = false;


    public MainCommandHandler() {
        workingDirectory = "";
        mScanner = new Scanner(System.in);
        mFileHandler = new FileHandler();
    }

    String lastInput;

    public void listenToCommand() {
        do {
            if (workingDirectory.isEmpty() || !mFileHandler.isValidDir(workingDirectory)) {
                print("Please set working directory");
                lastInput = mScanner.next();
                workingDirectory = lastInput;
            }
            else if(hasCsv == false) {
                print("No CSV found!! Please add CSV file and enter retry to try again");
                lastInput = mScanner.next();
                processCommand(lastInput);
            }

            else if(targetRow < 0) {
                print("Please set target data row from csv.");
                targetRow = mScanner.nextInt();
            }

            else {
                print("Please enter command, type help to show list of commands");
                lastInput = mScanner.next();
                processCommand(lastInput);
            }

        } while (true);
    }

    private void processCommand(String command) {
        try {
            if (command.equals("retry")){
               mFileHandler.processDirectory(new File(workingDirectory));
            }

            else if (command.equals("help")) {
                print("Valid commands:\n [update,stringAttribute]\n [set,stringAttribute]\n [delete,stringAttribute]");
            } else if (command.contains("set")) {
                String[] commandPair = command.split(",");
                String xmlTag = commandPair[1];
                mFileHandler.insertTranslationTag(xmlTag);
            } else if(command.contains("updateAdd")) {
                String[] commandPair = command.split(",");
                String xmlTag = commandPair[1];
                mFileHandler.appendTag(xmlTag, commandPair[2]);
            }else if(command.contains("updateReplace")) {
                String[] commandPair = command.split(",");
                String xmlTag = commandPair[1];
                mFileHandler.replaceTag(xmlTag);
            }

            else if (command.contains("update")) {
                String[] commandPair = command.split(",");
                String xmlTag = commandPair[1];
                mFileHandler.updateTranslationTag(xmlTag);

            } else if (command.contains("changeRow")) {
                print("Enter new row: ");
                targetRow = mScanner.nextInt();
                print("target row set to: " + targetRow);

            } else if (command.contains("exit")){
                print("exiting android localization tool ");
                System.exit(0);
            }
            else {
                print("Invalid Command syntax. Try again. Command: " + command);
            }


        } catch (Exception e) {
            print("Invalid Command syntax. Try again. Command: " + command);
        }
    }


    private static void print(String msg) {
        System.out.println(msg);
    }

    private static class FileHandler {
        private List<String> locales;
        private List<String> localeFolders;
        private HashMap<Integer, List<String>> dataRows;

        FileHandler() {
            locales = new ArrayList<>();
            dataRows = new HashMap<>();
            localeFolders = new ArrayList<>();
        }

        public boolean isValidDir(String path) {
            File file = new File(path);
            if (file.exists() == false) {
                print("invalid directory");
            } else {
                //Only load
                if (locales.isEmpty()) {
                    processDirectory(file);
                }
            }
            return file.exists();
        }


        private void insertTranslationTag(String tagName) {
            for (String folder : localeFolders) {
                String resStringFile = workingDirectory + "/" + folder + "/strings.xml";
                insertStringResource(tagName, resStringFile, getLocaleCodeFromFolderName(folder));
            }

        }

        private void updateTranslationTag(String tagName) {
            for (String folder : localeFolders) {
                String resStringFile = workingDirectory + "/" + folder + "/strings.xml";
                updateStringResource(tagName, resStringFile, getLocaleCodeFromFolderName(folder), false, null);
            }

        }

        private void appendTag(String tagName, String appendString) {
            for (String folder : localeFolders) {
                String resStringFile = workingDirectory + "/" + folder + "/strings.xml";
                updateStringResource(tagName, resStringFile, getLocaleCodeFromFolderName(folder),true, appendString);
            }
        }

        private void replaceTag(String tagName) {
            for (String folder : localeFolders) {
                String resStringFile = workingDirectory + "/" + folder + "/strings.xml";
                replaceNonBreakingSpaceToHtmlCode(tagName, resStringFile, getLocaleCodeFromFolderName(folder));
            }
        }

        private Element getTargetElement(NodeList nodeList, String resName) {
            Element result = null;

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                if (element != null) {
                    if (element.getAttribute("name").equals(resName)) {
                        result = element;
                    }
                }
            }

            return result;
        }

        private void replaceHtmlEscapes(NodeList stringNodes) {
            for (int i = 0; i < stringNodes.getLength(); i++) {
                Element element = (Element) stringNodes.item(i);
                if (element != null){
                    String content = element.getTextContent();
                    if(element.getAttribute("name").equals("fahrenheit_temperature")) {
                        content = content.replace("&amp;", "&");
                        element.setTextContent(content);
                        System.out.println("replacing " + "&# with " + content);
                    }

                }
            }

        }

        private void replaceNonBreakingSpaceToHtmlCode(String resName, String file, String locale){
            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(file);
                int translationIndex = getTranslationIndex(locale);
                if (translationIndex < 0) {
                    print("no translation column for: " + locale);
                    return;
                }

                Element root = document.getDocumentElement();
                NodeList nodeList = root.getElementsByTagName("string");

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    if (element != null){
                        String content = element.getTextContent();
                        if(element.getAttribute("name").equals(resName)) {
                            content = content.substring(0,content.length() -1);
                            content += "&#160;";
                            element.setTextContent(content);
                            print("Done adding tag to: " + file + " content : " + content);
                        }

                    }
                }
                //element.setTextContent(localizedText);


                //Save the tag
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
                //transformerFactory.setAttribute("indent-number",49);
                //transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(new File(file));
                transformer.transform(source, result);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void updateStringResource(String resName, String file, String locale, boolean isAppend, String appendString) {
            replaceHtmlEncoding(file);

            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(file);
                int translationIndex = getTranslationIndex(locale);
                if (translationIndex < 0) {
                    print("no translation column for: " + locale);
                    return;
                }

                String localizedText = dataRows.get(targetRow).get(translationIndex);
                localizedText = processStringForSave(localizedText);

                Element root = document.getDocumentElement();
                Element element = getTargetElement(root.getElementsByTagName("string"), resName);
                //Keep original html entity
                //replaceHtmlEscapes(root.getElementsByTagName("string"));
                if (element == null) {
                    element = document.createElement("string");
                    element.setAttribute("name", resName);
                    root.appendChild(element);
                }

                if (isAppend) {
                   localizedText = element.getTextContent() +  appendString;
                }

                element.setTextContent(localizedText);


                //Save the tag
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                //transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
                //transformerFactory.setAttribute("indent-number",49);
                //transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(new File(file));
                transformer.transform(source, result);
                print("Done adding tag to: " + file + " value : " + localizedText);

            } catch (Exception e) {
                e.printStackTrace();
            }

            putBackHtmlEncoding(file);
        }

        private void insertStringResource(String resourceName, String file, String locale) {
            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(file);
                int translationIndex = getTranslationIndex(locale);

                if (translationIndex < 0) {
                    print("no translation column for: " + locale);
                    return;
                }
                Element root = document.getDocumentElement();
                Element newTag = document.createElement("string");
                newTag.setAttribute("name", resourceName);
                String localizedText = dataRows.get(targetRow).get(translationIndex);
                localizedText = processStringForSave(localizedText);
                newTag.setTextContent(localizedText);
                root.appendChild(newTag);

                //Save the tag
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");


                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(new File(file));
                transformer.transform(source, result);
                print("Done adding tag to: " + file + " value : " + localizedText);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private String processStringForSave(String input) {
            //android string resources don't like aphostrophe
            String output = input.replace("'","\\'");
            //csv was formatted with pipes for comma, need to change it back
            output = output.replace("|",",");
            if(output.contains(":")){
                //output += " %1$s";
            }
            return output;
        }

        private String getLocaleCodeFromFolderName(String folderName) {
            String localeCode = folderName.substring(folderName.indexOf("-") + 1, folderName.length());
            print("locale : " + localeCode);
            return localeCode;
        }

        private int getTranslationIndex(String locale) {
            //we don't have croatia (hr) but  we have serbo croatia use that instead
            if(locale.equals("hr")) {
                locale = "sr";
            }
            int index = 0;
            for (String csvLocale : locales) {
                csvLocale = csvLocale.trim();
                if (csvLocale.equals(locale) || csvLocale.equalsIgnoreCase(locale)) {
                    return index;
                }
                index++;
            }
            return -1;
        }

        private void processDirectory(File dir) {
            localeFolders.clear();
            for (String name : dir.list()) {
                if (name.endsWith(".csv")) {
                    hasCsv = true;
                    parseCsv(name);
                }
                else if (name.contains("values")) {
                    if (isIExcludeList(name) == false) {
                        localeFolders.add(name);
                    }
                }
            }

        }

        private void replaceHtmlEncoding(String fileName) {
            print("replace html encoding ");

            try
            {
                File file = new File(fileName);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = "", oldtext = "";
                while((line = reader.readLine()) != null)
                {
                    oldtext += line + "\r\n";
                }
                reader.close();

                String replacedtext  = oldtext.replaceAll("&", "&amp;");
                FileWriter writer = new FileWriter(fileName);
                writer.write(replacedtext);
                writer.close();

            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }

        private void putBackHtmlEncoding(String fileName) {
            print("put back encoding");
            try
            {
                File file = new File(fileName);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = "", oldtext = "";
                while((line = reader.readLine()) != null)
                {
                    oldtext += line + "\r\n";
                }
                reader.close();

                String replacedtext  = oldtext.replaceAll("&amp;", "&");
                FileWriter writer = new FileWriter(fileName);
                writer.write(replacedtext);
                writer.close();

            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }



        private boolean isIExcludeList(String name) {
            if(name.contains("hdpi") || name.contains("w820dp") || name.contains("xxhdpi")) {
                return true;
            }
            return false;
        }

        private void parseCsv(String csvFile) {
            BufferedReader reader = null;
            String line = "";
            String splitBy = ",";
            String fullFile = workingDirectory + "/" + csvFile;
            try {

                reader = new BufferedReader(new FileReader(fullFile));
                int index = 0;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(splitBy);

                    if (index == 0) {
                        locales.addAll(Arrays.asList(data));
                    } else {
                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.addAll(Arrays.asList(data));

                        print("row " + index + " col 1 data:"  + arrayList.get(0));
                        dataRows.put(index, arrayList);
                    }
                    index++;
                }
                print("****** csv file read result:*********");
                print("locales size: " + locales.size());
                print("rows found:" + dataRows.size());

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }


}
