package com.ibm.icu.message2x;

import java.util.Locale;
import java.util.Map;

public class MessageFormatter {
	final Mf2DataModel dataModel;
	final Locale locale;
	final String pattern;

	public MessageFormatter(Mf2DataModel dataModel, Locale locale, String pattern) {
		super();
		this.dataModel = dataModel;
		this.locale = locale;
		this.pattern = pattern;
	}

	String format(Map<String,Object> arguments) {
		return "";
	}
	String	formatToString(Map<String,Object> arguments) {
		return "";
	}
	Mf2DataModel getDataModel() {
		return dataModel;
	}
	Locale getLocale() {
		return locale;
	}
	String getPattern() {
		return pattern;
	}

	
	public static class Builder {
		Locale locale;
		String pattern;

		MessageFormatter build() {
			Mf2DataModel dataModel = Parser.parse(pattern);
			return new MessageFormatter(dataModel, locale, pattern);
		}
//		Builder setDataModel(Mf2DataModel dataModel) {
//			
//		}
//		Builder setFunctionRegistry(Mf2FunctionRegistry functionRegistry) {
//			
//		}
		Builder setLocale(Locale locale) {
			this.locale = locale;
			return this;
		}

		Builder setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}
	}
}
