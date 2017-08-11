# MultipartFormParser

A JVM library for parsing a `multipart/form-data` encoded stream.  

## Getting Started

### Installation

I don't know, build the lib and use it?

### Example usage 

Given the body (InputStream) and boundary (from the ContentHeader) you create a `StreamingMultipartFormParts` 
object. That is an `Iterator` over `StreamingPart`s. You can use this Iterator if you want to stream bytes
directly from the form - for example if you think you are going to get very large files and want to send 
them directly to S3 or something.

Alternatively you can convert the Iterator into `Parts` using the `MultipartFormMap.formMap`. This is
a `Map` of `FieldName`s to `Part`s. To create these `Part`s the entire stream has to be parsed and the
contents of each `Part` stored in memory or on disk (the writeToDiskThreshold determines when to use
memory and when to write to disk)

```java
public class Example {
    public void example() {
        
        int maxStreamLength = 100_000; // maximum length of the stream, will throw exception if this is exceeded
        int writeToDiskThreshold = 1024; // maximum length of in memory object - if part is bigger then write to disk
        File temporaryFileDirectory = null; // use default temporary file directory
        String contentType = "multipart/form-data; boundary=------WebKitFormBoundary6LmirFeqsyCQRtbj"; // content type from HTTP header
        
        // you are responsible for closing the body InputStream
        try(InputStream body = new FileInputStream("examples/safari-example.multipart")) {
        
            byte[] boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length()).getBytes(ISO_8859_1);
            Iterable<StreamingPart> streamingParts = StreamingMultipartFormParts.parse(
                boundary, body, ISO_8859_1, maxStreamLength);
        
            try (Parts parts = MultipartFormMap.formMap(streamingParts, ISO_8859_1, writeToDiskThreshold, temporaryFileDirectory)) {
                Map<String, List<Part>> partMap = parts.partMap;
        
                Part articleType = partMap.get("articleType").get(0);
                System.out.println(articleType.fieldName); // "articleType"
                System.out.println(articleType.headers); // {Content-Disposition=form-data; name="articleType"}
                System.out.println(articleType.length); // 8 bytes
                System.out.println(articleType.isInMemory()); // true
                System.out.println(articleType.getString()); // "obituary"
        
                Part simple7bit = partMap.get("uploadManuscript").get(0);
                System.out.println(simple7bit.fieldName); // "uploadManuscript"
                System.out.println(simple7bit.fileName); // "simple7bit.txt"
                System.out.println(simple7bit.headers); // {Content-Disposition => form-data; name="uploadManuscript"; filename="simple7bit.txt"
                                                        // Content-Type => text/plain}
                System.out.println(simple7bit.length); // 8221 bytes
                System.out.println(simple7bit.isInMemory()); // false
                simple7bit.getNewInputStream(); // stream of the contents of the file
            } catch (IOException e) {
                // parsing can go wrong... handle it here
            }
            
        } catch (IOException e) {
            // general stream exceptions
        }
        
    }
}
```

Enjoy!

Development
-----------

Should be completely self contained. Tell me if it isn't

## Running the tests

`./gradlew clean test`

## Deployment

Wish I new!

## Built With

Gradlew!

## Contributing

Contact me, Tiest Vilee, and ask.

## Authors

* **Tiest Vilee** - *Initial work* - [TiestVilee](https://github.com/tiestvilee)

See also the list of [contributors](https://github.com/tiestvilee/multipart-form-parser/graphs/contributors) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* Ripped off from the [apache commons fileupload](https://github.com/apache/commons-fileupload) library
