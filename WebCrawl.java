import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawl {
    static ArrayList<String> pathHolder = new ArrayList<>();
    static ArrayList<String> absolutePathHolder = new ArrayList<>();
    static HashSet<String> srcHolder = new HashSet<>();
    static HashSet<String> invalidHolder = new HashSet<>();
    static HashMap<String,Integer> smallest = new HashMap<>();
    static HashMap<String,Integer> largest = new HashMap<>();
    static long smallestsize = 999999999;
    static int largestsize = 0;
    static String host;
    static URL url;
    static int htmlcount = 0;
    static HashMap<String,Date> pathnDate = new HashMap<>();
    static HashMap<String,Date> latest = new HashMap<>();
    static HashMap<String,Date> oldest = new HashMap<>();
    static HashMap<String,String> innerRedirection = new HashMap<>();
    static ArrayList<String> outsideServer = new ArrayList<>();
    static ArrayList<String> validPath = new ArrayList<>();
    static {
        try {
            url = new URL("http://comp3310.ddns.net:7880");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws SocketException, MalformedURLException {
        System.out.println("loading...");
        pathHolder.add(""); //The main page
        try {
            getUrl(""); // start crawling
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        //Following is to convert the path in standard url
        for(String path: pathHolder){
            URL absoluteUrl = new URL(url,path);
            if(!absolutePathHolder.contains(absoluteUrl.toString())){
                absolutePathHolder.add(absoluteUrl.toString());
            }
        }
        int totalURL = absolutePathHolder.size() +srcHolder.size();

        //Following is to convert the invalidPath in standard url
        ArrayList<String> absoluteInvalidUrlHolder = new ArrayList<>();
        for(String invalid:invalidHolder){
            if(!invalid.contains("http")){
                URL absoluteUrl = new URL(url,invalid);
                absoluteInvalidUrlHolder.add(absoluteUrl.toString());
            }
        }
        //initial the date for comparison
        Date latestDate = pathnDate.get("");
        Date oldestDate = pathnDate.get("");

        for(String path: pathnDate.keySet()){
            //get the latestDate and that page
            if (latestDate.compareTo(pathnDate.get(path))<0){
                latestDate = pathnDate.get(path);
                latest.clear();
                URL turl = new URL(url,path);
                latest.put(turl.toString(),latestDate);
            }
            //get the oldestDate and that page
            if(oldestDate.compareTo(pathnDate.get(path))>0){
                oldestDate = pathnDate.get(path);
                oldest.clear();
                URL turl = new URL(url,path);
                oldest.put(turl.toString(),oldestDate);
            }
        }
        String host = url.getHost();
        // identify the outside server
        for(String path: absolutePathHolder){
            URL tempurl = new URL(path);
            if(!host.equals(tempurl.getHost())){
                outsideServer.add(tempurl.toString());
            }
        }
        String largesturl = "";
        for(String lurl:largest.keySet()){
            largesturl = lurl;
        }
        String smallestUrl = "";
        for(String surl:smallest.keySet()){
            smallestUrl = surl;
        }
        String latestpage = "";
        String oldestpage = "";
        for(String lurl:latest.keySet()){
            latestpage = lurl;
        }
        for(String ourl:oldest.keySet()){
            oldestpage = ourl;
        }
        System.out.println("loading completed");
        System.out.println("Q1 totalURL: " + totalURL);
        System.out.println("Q2 Non-html-object: "+ srcHolder.size()+"     html count: " + htmlcount);
        System.out.println("Q3 the smallest page is " + smallestUrl+ " and its size is " +smallestsize + ". The largest page is " + largesturl +" and its size is  "+ largestsize);
        System.out.println("Q4 the latest page is " + latestpage+ " and date is " +latestDate.toString() + ". The oldest page is " + oldestpage +" and date is  "+ oldestDate.toString());
        System.out.println("Q5 404 not found list:" + absoluteInvalidUrlHolder);
        System.out.println("Q6 inner redirention includes: ");
        for(String url: innerRedirection.keySet()){
            System.out.println("From " + url + " to " + innerRedirection.get(url));
        }
        System.out.println("Q7 outsideServer: " + outsideServer);

    }
    public static void getUrl( String path) throws IOException, SocketException, ParseException {
        //connect socket
        Socket socket = new Socket();
        host = url.getHost();
        int port = url.getPort();
        SocketAddress add = new InetSocketAddress(host,port);
        socket.connect(add,3000);
        OutputStreamWriter streamWriter = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bufferedWriter = new BufferedWriter(streamWriter);
        bufferedWriter.write("GET "  + "/"+path + " HTTP/1.0\r\n");
        bufferedWriter.write("Host: " + host +"\r\n");
        bufferedWriter.write("\r\n");
        bufferedWriter.flush();
        InputStream is = socket.getInputStream();
        BufferedInputStream streamReader = new BufferedInputStream(is);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, "utf-8"));

        String line = null;
        Pattern pattern1 = Pattern.compile("<a.*href=.+</a>"); // This is to find out the content that have url
        Pattern pattern2 = Pattern.compile("\"(.*?)\"");// This is to find out the path
        Pattern pattern3 = Pattern.compile("404 Not Found"); // This is for 404 status code
        Pattern pattern4 = Pattern.compile("src=\"(.*?)\""); // This is for non-html object
        Pattern pattern6 = Pattern.compile("200 OK"); // This is for successful connection
        Pattern pattern7 = Pattern.compile("Last-Modified:(.*)"); // This is to retrieve date
        Pattern pattern8 = Pattern.compile(" (.*)GMT"); // Retrive date
        Pattern pattern9 = Pattern.compile("Content-Length: .*");// Retrive size
        Pattern pattern10 = Pattern.compile("Location: (.*)"); // Retrieve redirection
        while((line = bufferedReader.readLine())!= null)
        {
            Matcher matcher1 = pattern1.matcher(line);
            Matcher matcher3 = pattern3.matcher(line);
            Matcher matcher4 = pattern4.matcher(line);
            Matcher matcher6 = pattern6.matcher(line);
            Matcher matcher7 = pattern7.matcher(line);
            Matcher matcher9 = pattern9.matcher(line);
            Matcher matcher10 = pattern10.matcher(line);
            if(matcher6.find()){// if it is valid connection

                    htmlcount++;
                    validPath.add(path);

            }
            if(matcher9.find()){ // if it has size information
                if(validPath.contains(path)) { // if it is valid connection
                    int size = Integer.parseInt(matcher9.group().substring(16)); // retrieve the size
                    if(size>largestsize){ //update the largest page buffer
                        largestsize = size;
                        largest.clear();
                        URL templargestURL = new URL(url,path);
                        largest.put(templargestURL.toString(),size);
                    }
                    if(size<smallestsize){// update the smallest page buffer
                        smallestsize = size;
                        smallest.clear();
                        URL tempsmallURL = new URL(url,path);
                        smallest.put(tempsmallURL.toString(),size);
                    }
                }
            }
            if(matcher7.find()){ // if it has date information
                Matcher matcher8 = pattern8.matcher(line);
                if(matcher8.find()){// confirm it is date information and then store them
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ", Locale.US);
                    Date date = dateFormat.parse(matcher8.group().substring(1));
                    pathnDate.put(path,date);
                }
            }
            if(matcher10.find()){ // if there is inner redirection
                String insideRedirect = matcher10.group().substring(10); //redirect Location
                URL url1 = new URL(url,path);
                innerRedirection.put(url1.toString(), insideRedirect);
            }
            if(matcher3.find()){ //if it returns 404 not found
                invalidHolder.add(path);
            }
            else {
                if (matcher1.find()) { // if it is url information
                    Matcher matcher2 = pattern2.matcher(matcher1.group());
                    if (matcher2.find()) { //retrive the path
                        URL tempurl =  new URL(url,matcher2.group().replace("\"", ""));
                        if (!pathHolder.contains(tempurl.toString())) {
                            pathHolder.add(tempurl.toString());
                            if (tempurl.getHost().equals(host)) {
                                getUrl(tempurl.getPath()); // recursively visit all path
                            }
                        }
                    }
                }
                if(matcher4.find()){
                    srcHolder.add(matcher4.group());
                }
            }
        }
        bufferedReader.close();
        bufferedWriter.close();
        socket.close();
    }
}