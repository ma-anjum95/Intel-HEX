/*******************************************************************************************************	
 * 	The MIT License (MIT)
 *	Copyright (c) 2015 M. A. Anjum
 *	Email : ma.anjum95@gmail.com
 *	
 *	Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *	and associated documentation files (the "Software"), to deal in the Software without restriction,
 *	including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *	sublicense, and/or sell copies of the Software, and to permit persons to whom the Software 
 *	is furnished to do so, subject to the following conditions:
 *
 *	The above copyright notice and this permission notice shall be included in all copies 
 *	or substantial portions of the Software.
 *
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 *	INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
 *	AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 *	DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF 
 *	OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ********************************************************************************************************/
package com.maanjum95.intelhex;

/*
 *	This class represent a single Record of Intel HEX file.
 *	Record is a single line of Intel HEX file.
 *	Each line contains 6 fields of different bytes.
 *	1- ASCII colon ':' (one char)
 *	2- Byte Count (a hex pair/two chars) represents the number of bytes in data field
 *	3- Address[Big endian] (two hex pairs/four chars) represents the address from base limit.
 * 	4- Record Type (a hex pair/two chars) define meaning of data field
 * 	5- Data Sequence of n bytes (n hex pairs/2n chars)
 * 	6- Check Sum (a hex pair/two chars) for checking for errors in record.
 * */

/**
 * @author M A Anjum
 *
 */
public class IntelHexRecord {
	
	// The different type of records which is used in Intel HEX Records.
	public static final byte RECORD_DATA 						= 0x00;
	public static final byte RECORD_END_OF_FILE 				= 0x01;
	public static final byte RECORD_EXTENDED_SEGMENT_ADDRESS 	= 0x02;
	public static final byte RECORD_START_SEGMENT_ADDRESS 		= 0x03;
	public static final byte RECORD_EXTENDED_LINEAR_ADDRESS 	= 0x04;
	public static final byte RECORD_START_LINEAR_ADDRESS 		= 0x05;
	
	// The six fields of the Intel HEX Record as defined above in the comments.
	private byte byteCount;
	private byte addrH;
	private byte addrL;
	private byte recordType;
	private byte[] dataSequence;
	private byte checkSum;
	
	// String containing the record
	private String record;
	
	/*
	 * Takes in a string representing a Intel HEX Record.
	 * 
	 * @param record a string containing a single record
	 * 
	 * @throws CheckSumFailException, IncorrectRecordException
	 * */
	IntelHexRecord(String record) throws CheckSumFailException, IncorrectRecordException {
		this.checkValidityOfRecord(record);
		try { 	// Since the byteCount may be wrong and the size of record might be smaller
			this.readAndStoreRecordFields(record);	
		} catch (IndexOutOfBoundsException e) {
			throw new IncorrectRecordException("IndexOutOfBounds: byteCount may be greater than the actual size of record");
		}
		this.checkCheckSum();
	}
	
	/*
	 *	Initializes a Record with the given parameters
	 *	
	 *	@param byteCount The number of bytes in the data field
	 *	@param addrH The 8 upper bits of 16-bit address
	 *	@param addrL The 8 lower bits of 16-bit address
	 *	@param recordType The record type of the current record
	 *	@param dataSequence An array containing the data bytes 
	 *
	 *	@throws IncorrectRecordException
	 * */
	IntelHexRecord(byte byteCount, byte addrH, byte addrL,
			byte recordType, byte[] dataSequence) throws IncorrectRecordException {
		this.byteCount = byteCount;
		this.addrH = addrH;
		this.addrL = addrL;
		this.recordType = recordType;
		this.dataSequence = dataSequence;
		this.checkSum = this.calculateCheckSum();
		
		this.record = IntelHexRecord.makeRecord(byteCount, addrH, addrL, recordType, dataSequence);
	}
	
	/*
	 *	Makes a Record Based on Intel HEX File.
	 *
	 *	@param byteCount The number of bytes in the data field
	 *	@param addrH The 8 upper bits of 16-bit address
	 *	@param addrL The 8 lower bits of 16-bit address
	 *	@param recordType The record type of the current record
	 *	@param dataSequence An array containing the data bytes 
	 *
	 *	@throws IncorrectRecordException
	 *
	 *	@return the Intel HEX record string
	 * */
	public static String makeRecord(byte byteCount, byte addrH, byte addrL,
			byte recordType, byte[] dataSequence) throws IncorrectRecordException {
		String toReturn;
		
		// If the byteCount and the size of dataSquence are not same throw exception
		if (byteCount != 0 && (dataSequence == null || byteCount != dataSequence.length))
			throw new IntelHexRecord.IncorrectRecordException("byteCount does not match the length of dataSequence.");
		
		toReturn = String.format(":%02X%02X%02X%02X", byteCount, addrH, addrL, recordType);
		
		for (int i = 0; i < dataSequence.length; i++) {
			toReturn += String.format("%02X", dataSequence[i]);
		}
		
		toReturn += String.format("%02X", IntelHexRecord.calculateCheckSum(byteCount, addrH, addrL,
				recordType, dataSequence));
		return toReturn;
	}
	
	/*
	 *	toString method returns a string representation of the record.
	 *	
	 *	@return a string representation of the record
	 * */
	public String toString() {
		String toReturn;
		
		toReturn = String.format("Record: %02X\n", this.record);
		toReturn += String.format("Byte Count: %02X\n", this.byteCount);
		toReturn += String.format("Address High: %02X\n", this.addrH);
		toReturn += String.format("Address Low: %02X\n", this.addrL);
		toReturn += String.format("Record Type: %02X\n", this.recordType);
		toReturn += String.format("Check Sum: %02X\n", this.checkSum);
		toReturn += String.format("Record: %02X\n", this.record);
		
		toReturn += "Data Bytes:: \n";
		for (int i = 0; i < this.dataSequence.length; i++) {
			toReturn += String.format("Byte %03d: %02X\n", i, this.dataSequence[i]);
		}
		return toReturn;
	}
	
	/*
	 *	Checks to see if there is any characters not matching Hex representation
	 *	
	 *	@param record String of the record
	 *
	 *	@throw IncorrectRecordException
	 * */
	private void checkValidityOfRecord(String record) throws IncorrectRecordException {
		for (int i = 1, n = record.length(); i < n; i++) {
			char ch = record.charAt(i);
			if (ch >= '0' && ch <= '9')
				continue;
			else if (ch >= 'A' && ch <= 'F')
				continue;
			else if (ch >= 'a' && ch <= 'f')
				continue;
			else
				throw new IncorrectRecordException(ch, i);
		}
	}
	
	/*
	 *	This method stores the different fields of the record in the class variables.
	 *	
	 *	@param record String representing a single record
	 *
	 *	@throws IncorrectRecordException
	 * */
	private void readAndStoreRecordFields(String record) throws IncorrectRecordException {
		int i = 0; // index of the current record character
		
		// Storing the record in class variable
		this.record = record;
		
		// The first character of a record should be a ':'
		if (this.record.charAt(i++) != ':')
			throw new IncorrectRecordException("Record should start with a colon \":\"");
		
		// The next two character are the number of bytes
		this.byteCount = this.combineCharToByte(this.record.charAt(i++), this.record.charAt(i++));
		
		// Next are 2 bytes / 4 chars of address
		this.addrH = this.combineCharToByte(this.record.charAt(i++), this.record.charAt(i++));
		this.addrL = this.combineCharToByte(this.record.charAt(i++), this.record.charAt(i++));
		
		// Next is the record type 
		this.recordType = this.combineCharToByte(this.record.charAt(i++), this.record.charAt(i++));
		
		// Next are the data sequence
		this.dataSequence = new byte[this.byteCount];

		for (int j = 0; j < this.byteCount; i+=2, j++) {
			this.dataSequence[j] = this.combineCharToByte(this.record.charAt(i), this.record.charAt(i+1));
		}
		
		// Next is the actual record check sum
		this.checkSum = this.combineCharToByte(this.record.charAt(i++), this.record.charAt(i++));
	}
	
	/*
	 *	Calculates the CheckSum from the stored record data
	 *	
	 *	@return calculateCheckSum
	 * */
	private byte calculateCheckSum() {
		return IntelHexRecord.calculateCheckSum(this.byteCount, this.addrH, this.addrL,
				this.recordType, this.dataSequence);
	}
	
	/*
	 *	Calculates the CheckCum of the provided data
	 *
	 *	@param byteCount The number of bytes in the data field
	 *	@param addrH The 8 upper bits of 16-bit address
	 *	@param addrL The 8 lower bits of 16-bit address
	 *	@param recordType The record type of the current record
	 *	@param dataSequence An array containing the data bytes 
	 *	
	 *	@return calculate checkSum
	 * */
	public static byte calculateCheckSum(byte byteCount, byte addrH, byte addrL,
			byte recordType, byte[] dataSequence) {
		byte calculateCheckSum = (byte) (byteCount + addrH + addrL + recordType);
		
		if (dataSequence != null) {
			for (int i = 0, n = dataSequence.length; i < n; i++) {
				calculateCheckSum += dataSequence[i];
			}
		}
		calculateCheckSum = (byte) -calculateCheckSum; // taking the 2's compliment which is simple as taking negative
		
		return calculateCheckSum;
	}
	
	/*
	 *	Checks if the actual checkSum is equal to the calculated.
	 *	
	 *	@throws CheckSumFailException if the two CheckSums donot match
	 * */
	private void checkCheckSum() throws CheckSumFailException {
		byte calculatedCheckSum = this.calculateCheckSum();
		if (calculatedCheckSum != this.checkSum) 
			throw new CheckSumFailException(calculatedCheckSum, this.checkSum);
	}
	
	/*
	 *	Combines the ch1 and ch2 and returns the byte representation of them
	 *	eg. ch1 = A & ch2 = F, the result will be byte 0xAF
	 *
	 *	@param ch1 the 4 MSBits
	 *	@param ch2 the 4 LSBits
	 *
	 *	@return the byte representation of combination of ch1 & ch2
	 * */
	private byte combineCharToByte(char ch1, char ch2) {
		String temp = String.valueOf(ch1) + String.valueOf(ch2);
		byte toReturn = (byte) (Integer.parseInt(temp, 16) & 0xff);
		return toReturn;
	}
	
	/////////////////////////////// GETTER METHODS ////////////////////////////////
	/*
	 *	Returns the record formatted string.
	 *	
	 *	@return record
	 * */
	public String getRecord() {
		return this.record;
	}
	
	/*
	 *	Returns the number of data bytes.
	 *	
	 *	@return byteCount
	 * */
	public byte getByteCount() {
		return this.byteCount;
	}
	
	/*
	 *	Returns the 8 MSBits of 16-bit address.
	 *	
	 *	@return addrH
	 * */
	public byte getAddrH() {
		return this.addrH;
	}
	
	/*
	 *	Returns the 8 LSBits of 16-bit address.
	 *	
	 *	@return addrL
	 * */
	public byte getAddrL() {
		return this.addrL;
	}
	
	/*
	 *	Returns the record variable
	 *	
	 *	@return record
	 * */
	public byte getRecordType() {
		return this.recordType;
	}
	
	/*
	 *	Returns the data bytes in an array
	 *	
	 *	@return dataSequence
	 * */
	public byte[] getDataSequence() {
		return this.dataSequence;
	}
	
	/*
	 *	Returns the Check Sum
	 *	
	 *	@return checkSum
	 * */
	public byte getCheckSum() {
		return this.checkSum;
	}
	
	////////////////// EXCEPTION CLASSES USED BY THE IntelHexRecord CLASS /////////////////////
	public static class IncorrectRecordException extends Exception {
		/*
		 *	IncorrectRecordException: generic can be caused because of a number of reasons all having to do with an incorrect record
		 *
		 *	@param exception a string just to let the user know what went wrong
		 * */
		IncorrectRecordException(String exception) {
			super("IncorrectRecordException: " + exception);
		}
		
		/*
		 *	IncorrectRecordException: caused by a non hex character in the string excluding the very first colon ':'
		 *
		 *	@param ch the non-hex character.
		 *	@param index the index of non-hex character in the record string.
		 * */
		IncorrectRecordException(char ch, int index) {
			super(String.format("IncorrectRecordException: Incorrect character found: %c at index %d", ch, index));
		}
	}
	
	public static class CheckSumFailException extends Exception {
		/*
		 *	CheckSumFailException: caused by the difference in the acutal CheckSum and the calculated
		 *
		 *	@param calcCheckSum the calculated check sum
		 *	@param actualCheckSum the actual check sum
		 * */
		CheckSumFailException(byte calcCheckSum, byte actualCheckSum) {
			super(String.format("CheckSumException: The calculated checksum %X is not equal to one in the record %X.", calcCheckSum, actualCheckSum));
		}
	}
}
