package uk.ac.ox.oucs.vle;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class XcriErrorWriter extends OutputStreamWriter implements XcriLogWriter {
		
	private static String source;
	
	public XcriErrorWriter(OutputStream arg0, String source, String generated) throws IOException {
		super(arg0);
		
		XcriErrorWriter.source = source;
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    
		this.write("<html><head></head><body>" +
				"<h3>Errors and Warnings from SES Import ");
		this.write(sdf.format(cal.getTime()));
		this.write("</h3>");
		if (null != generated) {
			this.write("<h3>Using the XCRI file generated on ");
			this.write(generated);
			this.write("</h3>");
		}
		this.write("<pre>");
	}
	
	public void flush() throws IOException {
		this.write("</pre></body></html>");
		super.flush();
	}
	
	public String getIdName() {
		return source+"ImportError.html";
	}
	
	public String getDisplayName() {
		return source+"ImportErrorLog.html";
	}

}