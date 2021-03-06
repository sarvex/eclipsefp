/** 
 * Copyright (c) 2012 by JP Moresmau
 * This code is made available under the terms of the Eclipse Public License,
 * version 1.0 (EPL). See http://www.eclipse.org/legal/epl-v10.html
 */
package net.sf.eclipsefp.haskell.hlint;

import java.util.Locale;

/**
 * fixes HLint warning
 * @author JP Moresmau
 *
 */
public class HLintFixer {

	/**
	 * simple struct
	 *
	 */
	public static class HLintFix {
		/**
		 * the length of text that has to be replaced
		 */
		private int length;
		/**
		 * the value to use has replacement
		 */
		private String value="";
		
		/**
		 * have we fully matched the input
		 */
		private boolean fullMatch=false;
		
		/**
		 * @return the length
		 */
		public int getLength() {
			return length;
		}
		
		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}
		
		/**
		 * @return the fullMatch
		 */
		public boolean isFullMatch() {
			return fullMatch;
		}
	}
	
	public static boolean canFix(Suggestion s){
		if (s.getPre().getType().equals(CodeModificationType.TEXT)){
			if (s.getPost().getType().equals(CodeModificationType.TEXT)){
				String p=((CodeModificationText)s.getPost()).getText().trim();
				// we cannot replace automatically when we're just told to combine with another line
				if (p.length()>0 && p.toLowerCase(Locale.ENGLISH).startsWith("combine with") && Character.isDigit(p.charAt(p.length()-1))){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * fixes the given string
	 * @param doc the document content
	 * @param offset the offset given by the suggestion location
	 * @param s the suggestion given by hlint
	 * @return the fix structure
	 */
	public static HLintFix fix(String doc, int offset,Suggestion s){
		HLintFix fix=new HLintFix();
		if (s.getPre().getType().equals(CodeModificationType.TEXT)){
			String txt=((CodeModificationText)s.getPre()).getText();
			int offset2=matchIgnoreSpace(doc, offset, txt, 0);
			fix.fullMatch=offset2>-1;
			fix.length=offset2-offset;
		} else {
			fix.fullMatch=true;
		}
		if (s.getPost().getType().equals(CodeModificationType.TEXT)){
			fix.value=((CodeModificationText)s.getPost()).getText().trim();
		}
		if (offset>0){
			if (Character.isJavaIdentifierPart(doc.charAt(offset-1))){
				fix.value=" "+fix.value;	
			}
		}
		return fix;
	}
	
	/**
	 * match two strings ignoring spaces
	 * @param doc the first string to match
	 * @param offset the offset to start searching in the first string
	 * @param txt the second string
	 * @param txtIdx the offset to start searching in the second string
	 * @return the index at the end of the match in the first string
	 */
	private static int matchIgnoreSpace(String doc,int offset,String txt, int txtIdx){
		int txtIdx2=ignoreSpace(txt, txtIdx);
		if (txtIdx2<txt.length()){
			int offset2=ignoreSpace(doc, offset);
			if (offset2<doc.length()){
				while (doc.substring(offset2).startsWith("--")){
					int ix=doc.indexOf("\n",offset2);
					offset2=ignoreSpace(doc, ix);
				}
				char c1=doc.charAt(offset2);
				char c2=txt.charAt(txtIdx2);
				if (c1==c2){
					return matchIgnoreSpace(doc, offset2+1, txt, txtIdx2+1);
				} else if (c2=='(' || c2==')'){ // hlint sometimes adds extra brackets in the text it tells you to replace
					return matchIgnoreSpace(doc, offset2, txt, txtIdx2+1);
				} else {
					return -1;
				}
			}
			offset=offset2;
		}
		return offset;
	}
	
	/**
	 * skip spaces
	 * @param doc the text to skip spaces in
	 * @param offset the offset to start skipping
	 * @return the offset of the first non space character after offset
	 */
	private static int ignoreSpace(String doc,int offset){
		if (offset<doc.length()){
			char c=doc.charAt(offset);
			while (offset<doc.length() && Character.isWhitespace(c)){
				offset++;
				c=doc.charAt(offset);
			}
		}
		return offset;
	}
	
}
