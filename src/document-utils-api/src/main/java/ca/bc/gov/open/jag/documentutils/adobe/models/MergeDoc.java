package ca.bc.gov.open.jag.documentutils.adobe.models;

import java.util.UUID;

/**
 * 
 * Represents a single input document for merging.
 * 
 * @author shaunmillargov
 *
 */
public class MergeDoc {
	
	private String id; 
	private byte[] file;

	public MergeDoc(byte[] file) {
		UUID uniqueKey = UUID.randomUUID();
		this.id = uniqueKey.toString();
		this.file = file; 
	}
	
	public String getId() {
		return id;
	}
	
	public byte[] getFile() { return file; }

}
