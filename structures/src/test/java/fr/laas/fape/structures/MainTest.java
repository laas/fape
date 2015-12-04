package fr.laas.fape.structures;

import java.util.Arrays;

public class MainTest {

    @Ident(SimpleClass.class)
    public static class SimpleClass extends AbsIdentifiable {

        String f1;
        int f2;

        @ValueConstructor
        public SimpleClass(String f6, int f2) {
            this.f1 = f6;
            this.f2 = f2;
        }

        public SimpleClass() {

        }

        public String toString() {
            return f1 +" - "+f2;
        }
    }

    public static void main(String[] args) {
        class TestStore extends IRStorage {
            public SimpleClass getTest(String s, int i) { return (SimpleClass) get(SimpleClass.class, Arrays.asList(s, i)); }
            public SimpleClass getTest(int id) { return (SimpleClass) get(SimpleClass.class, id); }
        }

//        IRStorage store = new IRStorage() { public Test getTest(String s, int i) { return (Test) get(Test.class, Arrays.asList(s, i)); } };
        TestStore store = new TestStore();
//        Object ret = store.getNew(Test.class, Arrays.asList("azeaze", 8));
        System.out.println(store.getTest("coucou", 9));
        System.out.println(store.getTest("coucou", 8));
        System.out.println(store.getTest("coucou", 9));
        System.out.println(store.getTest("coucoulast", 8));
        System.out.println(store.getTest(2));
    }

}
