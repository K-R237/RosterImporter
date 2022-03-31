package com.circularkey.learnmobile;


import static com.codename1.ui.CN.*;
import com.codename1.ui.*;
import com.codename1.ui.Form;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.io.*;
import com.codename1.ui.Toolbar;
import java.io.*;
import org.apache.pdfbox.*;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class MyApp {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature, uncomment if you have a pro subscription
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    public void start() {
   	
        Toolbar.setGlobalToolbar(true);
        Form hi = new Form("Storage", new BoxLayout(BoxLayout.Y_AXIS));
        hi.getToolbar().addCommandToRightBar("+", null, (e) -> {
            TextField tf = new TextField("", "File Name", 20, TextField.ANY);
            TextArea body = new TextArea(5, 20);
            body.setHint("File Body");
            Command ok = new Command("OK");
            Command cancel = new Command("Cancel");
            Command result = Dialog.show("File Name", BorderLayout.north(tf).add(BorderLayout.CENTER, body), ok, cancel);
            if(ok == result) {
                try(OutputStream os = Storage.getInstance().createOutputStream(tf.getText())) {
                    os.write(body.getText().getBytes("UTF-8"));
                    createFileEntry(hi, tf.getText());
                    hi.getContentPane().animateLayout(250);
                } catch(IOException err) {
                    Log.e(err);
                }
            }
        });
        
        for(String file : Storage.getInstance().listEntries()) {
            createFileEntry(hi, file);
        }
        hi.show();
    }

    
    private void createFileEntry(Form hi, String file) {
       Label fileField = new Label(file);
       Button delete = new Button();
       Button view = new Button();
       FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
       FontImage.setMaterialIcon(view, FontImage.MATERIAL_OPEN_IN_NEW);
       Container content = BorderLayout.center(fileField);
       int size = Storage.getInstance().entrySize(file);
       content.add(BorderLayout.EAST, BoxLayout.encloseX(new Label(size + "bytes"), delete, view));            
       delete.addActionListener((e) -> {
           Storage.getInstance().deleteStorageFile(file);
           content.setY(hi.getWidth());
           hi.getContentPane().animateUnlayoutAndWait(150, 255);
           hi.removeComponent(content);
           hi.getContentPane().animateLayout(150);
       });         
       view.addActionListener((e) -> {
           try(InputStream is = Storage.getInstance().createInputStream(file)) {
               //String s = Util.readToString(is, "UTF-8");
               String s = convertPDFToTxt(file);
               cleanseRoster(s);
//TODO		   Buttons to export to calendar or cancel.
               Dialog.show(file, s, "Export to calendar", null);
           } catch(IOException err) {
               Log.e(err);
           }
       });
       hi.add(content);
    } 

    private String cleanseRoster(String s) {
		// TODO Auto-generated method stub
    	/* Parse the contents of the string to only return meaningful information: 
    	   dates, shift start and end times.*/
    	
    	//Roster is always 28 days - this array will store each day as an array size 2
    	//that includes the date and shift time for each day. Days off will contain
    	//an empty string.
		String[][] output = new String[28][2];
		
		for (int i = 0; i < output.length; i++) { //Initialise array with "" for each date
			output[i][1] = "";
		}
		
		int j = 0; // Counts which date we are up to for parsing the dates
		int k = 0; // Counts which date we are up to for parsing shifts

    	//Iterate through each character of the string
    	for(int i = 0; (i < s.length()); i++) {
			//If the character is a digit, check if it is a date by
    		//seeing if there is a '/' in the next two chars.
    		if (Character.isDigit(s.charAt(i))) {
				//Catch dates
    			if (s.charAt(i + 2) == '/' && j < 28) {
					output[j][0] = s.substring(i, i + 8);
					j++;
					i += 8;
				//Catch shifts
    			} else if (s.charAt(i+1) == 'a' || s.charAt(i+1) == 'p' || s.charAt(i+2) == 'a' || s.charAt(i+2) == 'p') {
    				while (!(Character.isWhitespace(s.charAt(i)))) {
    					output[k][1] += String.valueOf(s.charAt(i));
    					i++;
    				}
    				k++;
    			}
			} else if (s.charAt(i) == 'R') {
				//Catch recreation leave days
				if (s.charAt(i+1) == 'e' && s.charAt(i+2) == 'c') {
					k++;
					i += 10;
				//Catch rest days
				} else if (s.charAt(i+1) == 'D') {
					k++;
					i += 2;
				}
			} else if (s.charAt(i) == 'P') {
				//Catch programmed days off
				if (s.charAt(i+1) == 'D' && s.charAt(i+2) == 'O') {
					k++;
					i += 3;
				}
				//Catch sick leave days
			} else if (s.charAt(i) == 'S' && s.charAt(i+1) == 'i' && s.charAt(i+2) == 'c') {
				k++;
				i += 11;
			}
				
		}
    	
    	for (int i = 0; i < output.length; i++) {
    		System.out.print(output[i][0] + " ");
    		System.out.print(output[i][1] + "\n");
    	}
    	
    	return s;
	}

	public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }
    
    public static String convertPDFToTxt(String filePath) throws IOException {
    	//Take a PDF file and return it as a string containing only file text.
    	
        byte[] thePDFFileBytes = readFileAsBytes(filePath);
        PDDocument pddDoc = Loader.loadPDF(thePDFFileBytes);
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pddDoc);
        pddDoc.close();
        return pageText;
    }

	private static byte[] readFileAsBytes(String filePath) throws IOException {
		// Take a file, create an input stream and return the contents as bytes.
        
		//FileInputStream inputStream = new FileInputStream(filePath);
        InputStream inputStream = Storage.getInstance().createInputStream(filePath);
		return IOUtils.toByteArray(inputStream);
	}

}
