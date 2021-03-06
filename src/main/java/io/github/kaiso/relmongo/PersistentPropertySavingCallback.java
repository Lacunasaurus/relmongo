/**
*   Copyright 2018 Kais OMRI and authors.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package io.github.kaiso.relmongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Field;
import java.util.stream.Collectors;

public class PersistentPropertySavingCallback implements FieldCallback {

    private Object source;

    public PersistentPropertySavingCallback(Object source) {
        super();
        this.source = source;
    }

    public void doWith(Field field) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);

        if (field.isAnnotationPresent(OneToMany.class)) {
            String name = "";
            String referencedPropertyName = "";
            try {
                name = field.getAnnotation(JoinProperty.class).name();
                referencedPropertyName = field.getAnnotation(JoinProperty.class).referencedPropertyName();
            } catch (Exception e) {
                throw new IllegalArgumentException("Missing or misconfigured @JoinProperty annotation", e);
            }
            if (!"_id".equals(referencedPropertyName)) {
                throw new IllegalArgumentException("in @OneToMany, referencedPropertyName must be allways _id ");
            }
            Object ids = null;
            try {
                ids = ((BasicDBObject) source).get(name);
                if (ids instanceof BasicDBList) {
                    BasicDBList list = new BasicDBList();
                    list.addAll(((BasicDBList) ids).stream().map(this::keepOnlyIdentifier).collect(Collectors.toList()));
                    ((BasicDBObject) source).put(name,list);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Property defined in @JoinProperty annotation is not present", e);
            }

        }

    }

    private BasicDBObject keepOnlyIdentifier(Object obj) {
        return new BasicDBObject().append("_id", ((DBObject) obj).get("_id"));
    }

}
