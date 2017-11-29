package log;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

public class LogParse2 {

    public static void main(String []args){
        try{
            //将src下面的xml转换为输入流
            InputStream inputStream = new FileInputStream("src/main/resources/cardFile.xml");
            //创建SAXReader读取器，专门用于读取xml
            SAXReader saxReader = new SAXReader();
            //根据saxReader的read重写方法可知，既可以通过inputStream输入流来读取，也可以通过file对象来读取
            Document document = saxReader.read(inputStream);

            Element rootElement = document.getRootElement();
            Iterator<Element> modulesIterator = rootElement.elements("name").iterator();
            //rootElement.element("name");获取某一个子元素
            //rootElement.elements("name");获取根节点下子元素moudule节点的集合，返回List集合类型
            //rootElement.elements("module").iterator();把返回的list集合里面每一个元素迭代子节点，全部返回到一个Iterator集合中
            while(modulesIterator.hasNext()){
                Element moduleElement = modulesIterator.next();
                Element nameElement = moduleElement.element("name");
                System.out.println(nameElement.getName() + ":" + nameElement.getText());
                Element valueElement = moduleElement.element("value");
                System.out.println(valueElement.getName() + ":" + valueElement.getText());
                Element descriptElement = moduleElement.element("descript");
                System.out.println(descriptElement.getName() + ":" + descriptElement.getText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
