public class Test {
    public static void main(String[] args) {
        String str = " \t\tq\tqsdf sdfsdf  dsf\t\tsadasd \t";
        String[] splited = str.split("[ \t]+");
        System.out.println(splited.length);
        
    }
}
