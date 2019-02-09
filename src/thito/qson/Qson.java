package thito.qson;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
MIT License

Copyright (c) 2019 Thito Yalasatria S.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
/***
 * A simple JSON parser
 * 
 * 
 * @author Thito Yalasatria S.
 */
public class Qson {

	/***
	 * The default tab string
	 */
	public final static String DEFAULT_TAB = "\t";

	/***
	 * Check if an object is stringable using
	 * {@link java.lang.String#toString()}
	 * 
	 * @param o
	 * @return
	 */
	public static boolean isStringable(Object o) {
		if (o == null)
			return true;
		try {
			o.getClass().getField("TYPE");
			return true;
		} catch (final Exception e) {
		}
		return o.getClass().isPrimitive() || o instanceof String;
	}

	private boolean array;
	private final Map<Object, Qson> cache = new HashMap<>();
	private String json;
	private String[][] jsonValue;
	private Qson parent;
	private String tab = DEFAULT_TAB;

	/***
	 * Read a json object. From Map, List, Array, and Stringed Object.
	 * 
	 * @param o
	 */
	public Qson(Object o) {
		String j = new String();
		if (o == null) {
			json = "null";
			return;
		}
		if (o instanceof Map) {
			for (final Entry<?, ?> m : ((Map<?, ?>) o).entrySet()) {
				final String key = new Qson(m.getKey()).asJSON(false);
				final String value = new Qson(m.getValue()).asJSON(false);
				j += ", " + key + " : " + value;
			}
			if (!j.isEmpty())
				j = j.substring(2);
			json = "{" + j + "}";
		} else if (o instanceof List) {
			for (final Object ox : (List<?>) o) {
				j += ", " + new Qson(ox).asJSON(false);
			}
			if (!j.isEmpty())
				j = j.substring(2);
			json = "[" + j + "]";
		} else if (o instanceof Qson) {
			json = ((Qson) o).asJSON(false);
		} else if (o.getClass().isArray()) {
			final int length = Array.getLength(o);
			for (int i = 0; i < length; i++) {
				final Object ox = Array.get(o, i);
				j += ", " + new Qson(ox).asJSON(false);
			}
			if (!j.isEmpty())
				j = j.substring(2);
			json = "[" + j + "]";
		} else if (isStringable(o)) {
			json = String.valueOf(o);
		} else {
			final Map<String, Object> map = new HashMap<>();
			final Set<Field> fields = new HashSet<>();
			Class<?> cl = o.getClass();
			while (cl.getSuperclass() != null) {
				fields.addAll(Arrays.asList(cl.getDeclaredFields()));
				cl = cl.getSuperclass();
			}
			for (final Field f : fields) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				try {
					f.setAccessible(true);
					final Object result = f.get(o);
					if (result != null) {
						map.put(f.getName(), new Qson(result).asObject());
					}
				} catch (final Exception e) {
				}
			}
			for (final Entry<?, ?> m : ((Map<?, ?>) map).entrySet()) {
				final String key = new Qson(m.getKey()).asJSON(false);
				final String value = new Qson(m.getValue()).asJSON(false);
				j += ", " + key + " : " + value;
			}
			if (!j.isEmpty())
				j = j.substring(2);
			json = "{" + j + "}";
		}
	}

	/***
	 * Convert this object into an array
	 * 
	 * @return the array, the content could be anything except Qson object
	 */
	public Object[] asArray() {
		if (isNull())
			return null;
		return asList().toArray();
	}

	/***
	 * Convert this object into a Boolean object
	 * 
	 * @return a Boolean object
	 * @see java.lang.Boolean
	 * @see {@link #isBoolean()}
	 */
	public boolean asBoolean() {
		return Boolean.parseBoolean(json);
	}

	/***
	 * Convert this object into JSON string
	 * 
	 * @param pretty
	 *            Beauty formatting?
	 * @return the json
	 */
	public String asJSON(boolean pretty) {
		return asJSON(pretty, 0);
	}

	private String asJSON(boolean pretty, int tabindex) {
		if (isNull())
			return "null";
		values();
		if (!pretty) {
			if (array) {
				String built = new String();
				for (int i = 0; i < values().length; i++) {
					final Qson get = get(new Object[] { i });
					if (get == null)
						continue;
					built += "," + get.asJSON(pretty, tabindex + 1);
				}
				if (!built.isEmpty())
					built = built.substring(1);
				return "[" + built + "]";
			} else {
				String built = new String();
				for (final String[] values : values()) {
					final String key = values[0];
					final Qson qsonKey = new Qson(key);
					final Object qsonObject = qsonKey.asObject();
					if (values.length <= 1)
						return qsonObject instanceof String ? "\"" + qsonObject + "\"" : qsonKey.asString();
					final Qson get = get(qsonKey.asObject());
					if (get == null)
						return null;
					built += "," + new Qson(key).asJSON(true, tabindex + 1) + " : " + get.asJSON(pretty, tabindex + 1);
				}
				if (!built.isEmpty())
					built = built.substring(1);
				return "{" + built + "}";
			}
		}
		if (array) {
			String built = new String();
			for (int i = 0; i < values().length; i++) {
				final Qson get = get(new Object[] { i });
				if (get == null)
					continue;
				built += ",\n" + tabIndex(tabindex + 1) + get.asJSON(pretty, tabindex + 1);
			}
			if (!built.isEmpty())
				built = built.substring(1);
			return "[" + built + "\n" + tabIndex(tabindex) + "]";
		} else {
			String built = new String();
			for (final String[] values : values()) {
				final String key = values[0];
				final Qson qsonKey = new Qson(key);
				final Object qsonObject = qsonKey.asObject();
				if (values.length <= 1)
					return qsonObject instanceof String ? "\"" + qsonObject + "\"" : qsonKey.asString();
				final Qson get = get(new Object[] { qsonKey.asObject() });
				if (get == null)
					return null;
				built += ",\n" + tabIndex(tabindex + 1) + qsonKey.asJSON(pretty, tabindex + 1) + " : "
						+ get.asJSON(pretty, tabindex + 1);
			}
			if (!built.isEmpty())
				built = built.substring(1);
			return "{" + built + "\n" + tabIndex(tabindex) + "}";
		}
	}

	/***
	 * Convert this object into a list
	 * 
	 * @return the list, the content could be anything except Qson object
	 */
	public List<Object> asList() {
		if (isNull())
			return null;
		final ArrayList<Object> obj = new ArrayList<>();
		for (int i = 0; i < values().length; i++) {
			obj.add(get(new Object[] { i }).asObject());
		}
		return obj;
	}

	/***
	 * Convert this object into a map
	 * 
	 * @return the map, the key and the value could be anything except Qson
	 *         object
	 */
	public Map<Object, Object> asMap() {
		if (isNull())
			return null;
		final Map<Object, Object> map = new HashMap<>();
		for (final String[] s : values()) {
			final String key = s[0];
			final Qson keyQson = new Qson(key);
			final Object keyObject = keyQson.asObject();
			final Qson value = get(keyObject);
			map.put(keyObject, value.asObject());
		}
		return map;
	}

	/***
	 * Convert this object into a Number object
	 * 
	 * @return a Number object
	 * @see java.lang.Number
	 * @see {@link #isNumber()}
	 */
	public Number asNumber() {
		if (isNull())
			return null;
		final Double d = Double.parseDouble(json);
		if (json.contains(".")) {
			return d;
		}
		return d.longValue();
	}

	/***
	 * Convert this object into normal object, such as java.lang.String,
	 * java.lang.Integer, java.lang.Double, etc.
	 * 
	 * @return a specified object
	 */
	public Object asObject() {
		if (isNull())
			return null;
		if (json.startsWith("[") && json.endsWith("]")) {
			return asList();
		}
		if (json.startsWith("{") && json.endsWith("}")) {
			return asMap();
		}
		return isNumber() ? asNumber() : isBoolean() ? asBoolean() : asString();
	}

	/***
	 * Convert this object into a string. If it wasn't a string, it will
	 * stringed ({@link java.lang.String#toString()})
	 * 
	 * @return stringed object
	 */
	public String asString() {
		if (isNull())
			return null;
		if (json.startsWith("\"") && json.endsWith("\"") || json.startsWith("'") && json.endsWith("'")) {
			return json.substring(1, json.length() - 1);
		}
		return json;
	}

	/***
	 * Get json members/value
	 * 
	 * @param path
	 * @return
	 */
	public Qson get(Object... path) {
		Qson result = null;
		for (final Object element : path) {
			if (result == null) {
				result = single(element);
			} else {
				result = result.single(element);
			}
		}
		return result;
	}

	/***
	 * Get parent Qson.
	 * 
	 * @return parent, null if it is the root.
	 */
	public Qson getParent() {
		return parent;
	}

	public boolean isBoolean() {
		return json.equals("false") || json.equals("true");
	}

	/***
	 * Check if the json value is a null (not a stringed "null")
	 * 
	 * @return is null and not "null"
	 */
	public boolean isNull() {
		return json.trim().equals("null");
	}

	/***
	 * Check if the json value is number type
	 * 
	 * @return result
	 */
	public boolean isNumber() {
		try {
			Double.parseDouble(json);
		} catch (final Exception e) {
			return false;
		}
		return true;
	}

	private String[][] jsonItOut(String json) {
		final List<String[]> list = new ArrayList<>();
		for (final String s : splitOut(json, ',', json.startsWith("{") && json.endsWith("}") || array)) {
			if (array) {
				list.add(new String[] { s });
			} else {
				list.add(splitOut(s, ':', false));
			}
		}
		return list.toArray(new String[list.size()][]);
	}

	/***
	 * Set tab string, default {@link #DEFAULT_TAB}
	 * 
	 * @param tab
	 * @return
	 */
	public Qson setTab(String t) {
		if (t == null)
			throw new NullPointerException("tab");
		tab = t;
		cache.values().forEach(a -> {
			a.setTab(t);
		});
		return this;
	}

	/***
	 * Set the json value
	 * 
	 * @param o
	 *            the value, can be another {@link Qson}, {@link java.lang.Map},
	 *            {@link java.lang.Collection}, {@link java.lang.String}, or
	 *            {@link java.lang.Object} (Stringed
	 *            {@link java.lang.Object#toString()})
	 */
	public Qson setValue(Object o) {
		if (o instanceof String) {
			json = "\"" + o + "\"";
		} else if (o instanceof Qson) {
			json = ((Qson) o).asJSON(false, 0);
		} else if (o instanceof Map) {
			json = new Qson(o).asJSON(false, 0);
		} else if (o instanceof Collection) {
			json = new Qson(o).asJSON(false, 0);
		} else {
			json = String.valueOf(o);
		}
		jsonValue = null;
		return this;
	}

	private Qson single(Object o) {
		final Qson temp = cache.get(o);
		if (temp != null)
			return temp;
		for (int i = 0; i < values().length; i++) {
			final String values[] = values()[i];
			final String key = values[0];
			final Qson keyQson = new Qson(key);
			if (values.length <= 1) {
				if (o != null && o.equals(i)) {
					keyQson.parent = this;
					keyQson.setTab(tab);
					cache.put(i, keyQson);
					return keyQson;
				}
				continue;
			}
			final String value = values[1];
			if (keyQson.asObject().equals(o)) {
				final Qson n = new Qson(value);
				n.parent = this;
				n.setTab(tab);
				cache.put(keyQson.asObject(), n);
				return n;
			}
		}
		return null;
	}

	/***
	 * Split a string but ignore something inside square brackets, curly
	 * brackets, double quotes, and single quotes.
	 * 
	 * @param s
	 *            the target string
	 * @param ch
	 *            the delimiter
	 * @param skip
	 *            Skip first and last character
	 * @return split result
	 */
	private String[] splitOut(String s, char ch, boolean skip) {
		if (skip && s.length() > 1) {
			s = s.substring(1, s.length() - 1);
		}
		final List<String> list = new ArrayList<>();
		int cblevel = 0;
		int blevel = 0;
		boolean sq = false;
		boolean dq = false;
		String b = new String();
		int i = 0;
		char last = 0;
		for (final char c : s.toCharArray()) {
			if (!sq && !dq && last != '\\') {
				if (c == '{')
					cblevel++;
				if (c == '}')
					cblevel--;
				if (c == '[')
					blevel++;
				if (c == ']')
					blevel--;
			}
			if (cblevel < 0)
				throw new RuntimeException("unbalanced curly bracket: " + cblevel + " (" + s + ") [" + ch + "]");
			if (blevel < 0)
				throw new RuntimeException("unbalanced bracket: " + blevel + " (" + s + ") [" + ch + "]");
			if (c == '\'' && !dq && last != '\\')
				sq = !sq;
			if (c == '"' && !sq && last != '\\')
				dq = !dq;
			if (c == ch && cblevel == 0 && blevel == 0 && !dq && !sq || i >= s.length() - 1) {
				if (c != ch)
					b += c;
				list.add(b.trim());
				b = new String();
			} else
				b += c;
			i++;
			last = c;
		}
		if (cblevel != 0)
			throw new RuntimeException("unbalanced curly bracket: " + cblevel + " (" + s + ") [" + ch + "]");
		if (blevel != 0)
			throw new RuntimeException("unbalanced bracket: " + blevel + " (" + s + ") [" + ch + "]");
		if (sq)
			throw new RuntimeException("unbalanced single quote (" + s + ")");
		if (dq)
			throw new RuntimeException("unbalanced double quote (" + s + ") [" + ch + "]");
		return list.toArray(new String[list.size()]);
	}

	/***
	 * Iterate and build tabs
	 * 
	 * @param tabindex
	 *            amount of tabs
	 * @return strings that only contains tab
	 */
	private String tabIndex(int tabindex) {
		String tabs = new String();
		for (int i = 0; i < tabindex; i++) {
			tabs += tab;
		}
		return tabs;
	}

	/***
	 * String the object. Does not same as {@link #asString()}
	 * 
	 * @return a string
	 * @see {@link #asString()}
	 * @see {@link java.lang.Object#toString()}
	 */
	@Override
	public String toString() {
		if (isNull())
			return "Qson={value=null}";
		values();
		return "Qson={value=" + asJSON(false) + ", array=" + array + "}";
	}

	/***
	 * Get the raw parsed json. (Lazy Getter)
	 * 
	 * @return array of array mapping
	 */
	protected String[][] values() {
		if (jsonValue == null) {
			array = json.startsWith("[") && json.endsWith("]");
			jsonValue = jsonItOut(json);
		}
		return jsonValue;
	}
}
