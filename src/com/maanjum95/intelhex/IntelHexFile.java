/*******************************************************************************************************	
 * 	The MIT License (MIT)
 *	Copyright (c) 2015 Muhammad A. Anjum
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.maanjum95.intelhex.IntelHexRecord.CheckSumFailException;
import com.maanjum95.intelhex.IntelHexRecord.IncorrectRecordException;

public class IntelHexFile {

	/*
	 *	Writes the records to a file having path=filePath and name=fileName.hex
	 *	The .hex extension is automatically added and is not required in the parameter.
	 *	
	 *	@param filePath The path to the directory of the file.
	 *	@param fileName The name of the file; a .hex extension will be added.
	 *	@param records An array containing the IntelHexRecord to write.
	 *	
	 *	@throws IOException
	 * */
	public static void writeRecordsToFile(String filePath, String fileName, IntelHexRecord[] records) 
			throws IOException {
		File file = new File(filePath, fileName + ".hex");
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		
		for (int i = 0; i < records.length; i++) {
			bw.write(records[i].getRecord() + "\r\n"); // \r\n for cross platform hex file
		}
		bw.close();
	}

	/*
	 *	Reads Intel HEX records from the file and returns an array of IntelHEXRecords.
	 *
	 *	@param filePathName the complete path to file including its name and extension
	 *
	 *	@throws FileNotFoundException
	 *	@throws IOException
	 *	@throws CheckSumFailException
	 *	@throws IncorrectRecordException	
	 *
	 *	@return an array of IntelHexRecord parsed from the file.
	 * */
	public static IntelHexRecord[] readRecordsFromFile(String filePathName) 
			throws FileNotFoundException, IOException, CheckSumFailException, IncorrectRecordException  {
		IntelHexRecord[] toReturn = null;
		ArrayList<String> stringRecords = new ArrayList<String>();
		
		// Reading the records from the file
		File file = new File(filePathName);	
		BufferedReader br = new BufferedReader(new FileReader(file));		
		for(String line; (line = br.readLine()) != null; ) {
			stringRecords.add(line);
		}
		br.close();
		
		// Initializing the return array and filling it with the IntelHexRecords generated
		// from the input file lines
		toReturn = new IntelHexRecord[stringRecords.size()];
		for (int i = 0, n = toReturn.length; i < n; i++) {
			toReturn[i] = new IntelHexRecord(stringRecords.get(i));
		}		
		return toReturn;
	}
}
