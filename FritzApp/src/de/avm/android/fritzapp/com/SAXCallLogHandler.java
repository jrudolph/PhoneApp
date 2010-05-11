package de.avm.android.fritzapp.com;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.avm.android.fritzapp.model.Call;
import de.avm.android.fritzapp.model.CallLog;
import de.avm.android.fritzapp.model.ContactNumber.NUMBER_TYPE;
import de.avm.android.fritzapp.util.error.DataMisformatException;

/*
 * SAX-Handler for AVM Calllist-Format
 */
public class SAXCallLogHandler extends DefaultHandler {

	private boolean in_call = false;
	private boolean in_id = false;
	private boolean in_type = false;
	private boolean in_caller = false;
	private boolean in_called = false;
	private boolean in_name = false;
	private boolean in_numbertype = false;
	private boolean in_device = false;
	private boolean in_date = false;
	private boolean in_duration = false;
	private boolean in_count = false;

	private CallLog callLog;
	private Call currentCall;

	/**
	 * Instantiates a new sAX call log handler.
	 * 
	 * @param callLog
	 *            the call log
	 */
	public SAXCallLogHandler(CallLog callLog) {
		this.callLog = callLog;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (localName.toLowerCase().equals("call")) {
			currentCall = new Call();
			this.in_call = true;
		}
		if (localName.toLowerCase().equals("type")) {
			this.in_type = true;
		}
		if (localName.toLowerCase().equals("id")) {
			this.in_id = true;
		}
		if (localName.toLowerCase().equals("caller")) {
			this.in_caller = true;
		}
		if (localName.toLowerCase().equals("called")) {
			this.in_called = true;
		}
		if (localName.toLowerCase().equals("name")) {
			this.in_name = true;
		}
		if (localName.toLowerCase().equals("numbertype")) {
			this.in_numbertype = true;
		}
		if (localName.toLowerCase().equals("device")) {
			this.in_device = true;
		}
		if (localName.toLowerCase().equals("date")) {
			this.in_date = true;
		}
		if (localName.toLowerCase().equals("duration")) {
			this.in_duration = true;
		}
		if (localName.toLowerCase().equals("count")) {
			this.in_count = true;
		}
}

	/**
	 * on closing tags like: </tag>.
	 * 
	 * @param namespaceURI
	 *            the namespace uri
	 * @param localName
	 *            the local name
	 * @param qName
	 *            the q name
	 * 
	 * @throws SAXException
	 *             the SAX exception
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		if (localName.toLowerCase().equals("call")) {
			callLog.addCall(currentCall);
			this.in_call = false;
		}
		if (localName.toLowerCase().equals("id")) {
			this.in_id = false;
		}
		if (localName.toLowerCase().equals("type")) {
			this.in_type = false;
		}
		if (localName.toLowerCase().equals("caller")) {
			this.in_caller = false;
		}
		if (localName.toLowerCase().equals("called")) {
			this.in_called = false;
		}
		if (localName.toLowerCase().equals("name")) {
			this.in_name = false;
		}
		if (localName.toLowerCase().equals("numbertype")) {
			this.in_numbertype = false;
		}
		if (localName.toLowerCase().equals("device")) {
			this.in_device = false;
		}
		if (localName.toLowerCase().equals("date")) {
			this.in_date = false;
		}
		if (localName.toLowerCase().equals("duration")) {
			this.in_duration = false;
		}
		if (localName.toLowerCase().equals("count")) {
			this.in_count = false;
		}
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>.
	 * 
	 * @param ch
	 *            the ch
	 * @param start
	 *            the start
	 * @param length
	 *            the length
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		String str = new String(ch, start,length);
		String string = str;
		if (string == null) {
			string = "";
		}
		
		if (this.in_id) {
			currentCall.setId(string);

		}
		if (this.in_type) {
			currentCall.setType(Call.getCallTypeForKey(string));
		}
		if (this.in_caller) {
			currentCall.setPartnerNumber(string);
		}
		if (this.in_called) {
			currentCall.setInternNumber(string);
		}
		if (this.in_name) {
			currentCall.setPartnerName(string);
		}
		if (this.in_numbertype) {
			currentCall.setNumberType(NUMBER_TYPE.getNumberTypeForKey(string));
		}
		if (this.in_device) {
			currentCall.setInternPort(string);
		}
		if (this.in_date) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm");
			try {
				currentCall.setTimeStamp(formatter.parse(string));
			} catch (ParseException e) {
				throw new DataMisformatException("Invalid DateType", e);
			}
		}
		if (this.in_duration) {
			currentCall.setDuration(parseDuration(string));
		}
		if (this.in_count) {
			currentCall.setCount(Integer.getInteger(string, 0));
		}
	}

	/**
	 * Parses the duration.
	 * 
	 * @param durationFormatted
	 *            the duration formatted
	 * 
	 * @return the integer
	 */
	protected static Integer parseDuration(String durationFormatted) {
		final int[] time_multiplicators = { 1, 60, 1440 };
		int sum = 0;

		String[] parts = durationFormatted.split(":");
		if (parts.length == 0 || parts.length > 3) {
			throw new DataMisformatException("Duration not valid");
		}
		int j = 0;
		for (int i = parts.length - 1; i >= 0; i--) {
			sum += Integer.parseInt(parts[i]) * time_multiplicators[j];
			j++;
		}
		return sum;
	}

}
