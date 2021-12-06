package notion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public abstract class Properties {
    public String field;
    public String content;

    public Properties(String field, String content) {
        this.field = field;
        this.content = content;
    }

    public abstract JSONObject toJsonObject();

    @Override
    public String toString() {
        return toJsonObject().toJSONString();
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static class Title extends Properties {

        public Title(String field, String content) {
            super(field, content);
        }

        @Override
        public JSONObject toJsonObject() {
            JSONObject text = new JSONObject();
            text.put("content", getContent());

            JSONObject title = new JSONObject();
            title.put("type", "text");
            title.put("text", text);

            JSONArray titleArray = new JSONArray();
            titleArray.add(title);

            JSONObject titleObject = new JSONObject();
            titleObject.put("type", "title");
            titleObject.put("title", titleArray);
            return titleObject;
        }
    }

    public static class RichText extends Properties {

        public RichText(String field, String content) {
            super(field, content);
        }

        @Override
        public JSONObject toJsonObject() {
            JSONObject text = new JSONObject();
            text.put("content", getContent());

            JSONObject richText = new JSONObject();
            richText.put("type", "text");
            richText.put("text", text);

            JSONArray richTextArray = new JSONArray();
            richTextArray.add(richText);

            JSONObject richTextObject = new JSONObject();
            richTextObject.put("type", "rich_text");
            richTextObject.put("rich_text", richTextArray);
            return richTextObject;
        }
    }

    public static class Number extends Properties {

        public Number(String field, String content) {
            super(field, content);
        }

        @Override
        public JSONObject toJsonObject() {
            JSONObject numberObject = new JSONObject();
            numberObject.put("type", "number");
            numberObject.put("number", Double.valueOf(getContent()));
            return numberObject;
        }
    }

    public static class Date extends Properties {

        public Date(String field, String content) {
            super(field, content);
        }

        @Override
        public JSONObject toJsonObject() {
            JSONObject object = new JSONObject();
            object.put("start", getContent());

            JSONObject dateObject = new JSONObject();
            dateObject.put("type", "date");
            dateObject.put("date", object);
            return dateObject;
        }
    }
}
