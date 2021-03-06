/**
 * @(#)PDFBoxUtilities.java
 * Copyright (c) 2012, B.C. Ministry of Attorney General.
 * All rights reserved.
 */
package ca.bc.gov.open.jag.documentutils.utils;

import ca.bc.gov.open.jag.documentutils.exception.MergeException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.form.PDXFAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 
 * PDF Box Utilities 
 * 
 * @author shaunmillargov
 *
 */
public class PDFBoxUtilities {

	private final static Logger logger = LoggerFactory.getLogger(PDFBoxUtilities.class);	

	/**
	 * isPDFXfa() - returns boolean indicating if document is XFA (Smartform) type. 
	 * 
	 * @param pdfFile
	 * @return
	 */
	public static boolean isPDFXfa(byte[] pdfFile) {
		
		boolean isXFA = false;
		PDDocument doc = null;
		PDXFAResource xfa = null;
		try {
			doc =  getPDDocFromBytes(pdfFile);
			if ( null != doc.getDocumentCatalog().getAcroForm()) {
				xfa = doc.getDocumentCatalog().getAcroForm().getXFA();
			} 
			return null != xfa;
		} catch (MergeException | NullPointerException e) {
			logger.error(e.getMessage());
		} finally {			
			try {
				doc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return isXFA;

	}
	
	/**
	 * 
	 * getPDDocFromBytes() - loads a PDDocument from a byte array. 
	 * 
	 * @param pdfFile
	 * @return
	 * @throws MergeException
	 */
	private static PDDocument getPDDocFromBytes(byte[] pdfFile) {
		
		ByteArrayInputStream bis = new ByteArrayInputStream(pdfFile);
			
		try {
			return PDDocument.load(bis);
		} catch (InvalidPasswordException e) {
			throw new MergeException("Password Protected PDF.", e.getCause());
		} catch (IOException e) {
			throw new MergeException("File not a PDF.", e.getCause());
		}
		
	}

}
