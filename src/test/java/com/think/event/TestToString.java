package com.think.event;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Test;

public class TestToString {

    @Test
    public void testToString() {
        Student student = new Student();
        student.setId(10010100);
        student.setName("张三");
        String string = ReflectionToStringBuilder.toString(student);
        System.out.println(string);
    }

    public static class Student {
        private long id;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
