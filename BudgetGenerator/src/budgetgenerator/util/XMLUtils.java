/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package budgetgenerator.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Gustavo
 */
public class XMLUtils {

    public static String mapObjectInXML(Object obj) throws IllegalArgumentException, IllegalAccessException {
        StringBuilder xml = new StringBuilder();
        if (obj != null) {
            xml.append("<" + obj.getClass().getSimpleName().toLowerCase()+ ">");
            Class cls = obj.getClass();
            Field[] fields = cls.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);

                if (f.getType().isAssignableFrom(List.class)) {
                    xml.append("<" + f.getName() + ">");
                    Collection list = (Collection) f.get(obj);
                    for (Object objList : list) {
                        xml.append(mapObjectInXML(objList));
                    }
                    xml.append("</" + f.getName() + ">");
                } else {
                    xml.append("<" + f.getName() + ">");
                    xml.append(f.get(obj));
                    xml.append("</" + f.getName() + ">");
                }
            }
            xml.append("</" + obj.getClass().getSimpleName().toLowerCase()+ ">");
        }

        return xml.toString();
    }

}
