package fr.laas.fape.structures;

import java.util.Arrays;

public class HStructs {

    @Ident(Top.class)
    public static class Top extends AbsIdentifiable {

    }

    @Ident(Top.class)
    public static class BotInt extends Top {
        final int val;
        @ValueConstructor
        public BotInt(int val) {
            this.val = val;
        }
    }

    @Ident(Top.class)
    public static class BotString extends Top {
        final String v;
        @ValueConstructor
        public BotString(String v) {
            this.v = v;
        }
    }

    public static void main(String[] args) {
        IRStorage store = new IRStorage();

        store.get(BotString.class, Arrays.asList("coucou"));
        store.get(BotInt.class, Arrays.asList(1));
        store.get(BotString.class, Arrays.asList("coucou"));

        System.out.println(store.get(Top.class, 0));
        System.out.println(store.get(Top.class, 1));


    }
}
