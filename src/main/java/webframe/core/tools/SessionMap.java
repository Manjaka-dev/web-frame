package webframe.core.tools;

import jakarta.servlet.http.HttpSession;
import java.util.*;

/**
 * Une implémentation de Map qui agit comme un proxy vers HttpSession.
 * Toute modification sur cette map (put, remove, clear) est 
 * immédiatement répercutée dans la session HTTP de l'utilisateur.
 */
public class SessionMap implements Map<String, Object> {
    
    private final HttpSession session;

    public SessionMap(HttpSession session) {
        this.session = session;
    }

    @Override
    public int size() {
        int count = 0;
        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            e.nextElement();
            count++;
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return !session.getAttributeNames().hasMoreElements();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) return false;
        return session.getAttribute(key.toString()) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            if (Objects.equals(session.getAttribute(e.nextElement()), value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        if (key == null) return null;
        return session.getAttribute(key.toString());
    }

    @Override
    public Object put(String key, Object value) {
        Object old = session.getAttribute(key);
        session.setAttribute(key, value);
        return old;
    }

    @Override
    public Object remove(Object key) {
        if (key == null) return null;
        Object old = session.getAttribute(key.toString());
        session.removeAttribute(key.toString());
        return old;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        for (Entry<? extends String, ?> e : m.entrySet()) {
            session.setAttribute(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            session.removeAttribute(e.nextElement());
        }
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            keys.add(e.nextElement());
        }
        return keys;
    }

    @Override
    public Collection<Object> values() {
        List<Object> vals = new ArrayList<>();
        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            vals.add(session.getAttribute(e.nextElement()));
        }
        return vals;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new HashSet<>();
        Enumeration<String> e = session.getAttributeNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            entries.add(new AbstractMap.SimpleEntry<>(key, session.getAttribute(key)));
        }
        return entries;
    }
}
