
public class HttpUtils {

//    @Value("${product.url}")
//    private String url;

    private static final Logger logger= LoggerFactory.getLogger(HttpUtils.class);

    public String postData(String url, String json) {

        logger.info("请求地址："+ url);

        //创建 httpclient对象，用于做连接
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url); //生产地址

        //设置超时时间 请求超时时间 5s
        RequestConfig config = RequestConfig.custom()
                // 设置连接超时时间(单位毫秒)
                .setConnectTimeout(5000)
                // 设置请求超时时间(单位毫秒)
                .setConnectionRequestTimeout(5000)
                // socket读写超时时间(单位毫秒)
                .setSocketTimeout(5000) .build();

        httpPost.setConfig(config);


        if(httpPost == null){
            logger.info("连接失败");
            throw new BusinessException("连接失败");
        }
        logger.info("连接成功");


        //设置json
        StringEntity requestEntity = null;
        requestEntity = new StringEntity(json,"UTF-8");

        requestEntity.setContentEncoding("UTF-8");
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(requestEntity);

        // 响应模型
        HttpResponse response = null;
        String result = "";
        try {
            // 由客户端执行(发送)Post请求
            logger.info("请求报文:" + json);
            logger.info("开始请求————————");
            logger.info("请求中------------------");

            Long start = System.currentTimeMillis();  //开始时间
            try {
                response = httpClient.execute(httpPost);  //发送请求
            } catch (SocketTimeoutException s){
                s.printStackTrace();
                logger.info("请求超时");
                throw new BusinessException("请求超时");
            } catch (IOException e) {
                e.printStackTrace();
            }

            Long end = System.currentTimeMillis();  //结束时间
            logger.info("请求结束————————");
            logger.info("响应时间："+ String.valueOf(end - start));

            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            System.out.println("响应状态为:" + response.getStatusLine());
            if (responseEntity != null) {
                try {
                    result = EntityUtils.toString(responseEntity,"UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("响应内容为:" + result);
            }
        }  finally {

            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public String getData(String url) {

        logger.info("请求地址："+ url);

        //创建 httpclient对象，用于做连接
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(url); //生产地址

        //设置超时时间 请求超时时间 5s
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(5000).build();
        httpGet.setConfig(config);


        if(httpGet == null){
            logger.info("连接失败");
            throw new BusinessException("连接失败");
        }
        logger.info("连接成功");


        //设置json
        StringEntity requestEntity = null;
        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // 响应模型
        HttpResponse response = null;
        String result = "";
        try {
            // 由客户端执行(发送)get
            logger.info("开始请求————————");
            logger.info("请求中------------------");

            Long start = System.currentTimeMillis();  //开始时间
            try {
                response = httpClient.execute(httpGet);  //发送请求
            } catch (SocketTimeoutException s){
                logger.info("请求超时");
                throw new BusinessException("请求超时");
            } catch (IOException e) {
                e.printStackTrace();
            }

            Long end = System.currentTimeMillis();  //结束时间
            logger.info("请求结束————————");
            logger.info("响应时间："+ String.valueOf(end - start));

            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            System.out.println("响应状态为:" + response.getStatusLine());
            if (responseEntity != null) {
                try {
                    result = EntityUtils.toString(responseEntity,"UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("响应内容为:" + result);
            }
        }  finally {

            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     *
     * @param url 路径先拼接好
     */
    public String doGet(String url) {

        logger.info("请求地址："+url);
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        // 创建Get请求
        HttpGet httpGet = new HttpGet(url);
        // 响应模型
        String result = "";
        CloseableHttpResponse response = null;
        try {
            // 配置信息
            RequestConfig requestConfig = RequestConfig.custom()
                    // 设置连接超时时间(单位毫秒)
                    .setConnectTimeout(5000)
                    // 设置请求超时时间(单位毫秒)
                    .setConnectionRequestTimeout(5000)
                    // socket读写超时时间(单位毫秒)
                    .setSocketTimeout(5000) .build();

            // 将上面的配置信息 运用到这个Get请求里
            httpGet.setConfig(requestConfig);

            Long start = System.currentTimeMillis();  //开始时间
            // 由客户端执行(发送)Get请求
            response = httpClient.execute(httpGet);
            Long end = System.currentTimeMillis();  //开始时间
            logger.info("响应时间：" + String.valueOf(end - start) + "ms");

            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            System.out.println("响应状态为:" + response.getStatusLine());
            if (responseEntity != null) {
                System.out.println("响应内容长度为:" + responseEntity.getContentLength());
                result = EntityUtils.toString(responseEntity,"UTF-8");   //UTF-8 字符集
                System.out.println("响应内容为:" + result);
            }
        }
        catch (SocketTimeoutException e){
            logger.info("调用接口超时");
            throw new BusinessException("调用接口超时");
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 释放资源
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 发送 post请求，非json
     * @param url
     * @param params
     * @return
     */
    public String postData(String url,  List<NameValuePair> params) {

        logger.info("请求地址："+ url);

        //创建 httpclient对象，用于做连接
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url); //生产地址

        //设置超时时间 请求超时时间 5s
        RequestConfig config = RequestConfig.custom()
                // 设置连接超时时间(单位毫秒)
                .setConnectTimeout(5000)
                // 设置请求超时时间(单位毫秒)
                .setConnectionRequestTimeout(5000)
                // socket读写超时时间(单位毫秒)
                .setSocketTimeout(5000) .build();

        httpPost.setConfig(config);


        if(httpPost == null){
            logger.info("连接失败");
            throw  new BusinessException("连接失败");
        }
        logger.info("连接成功");


        //设置json
        //StringEntity requestEntity = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //requestEntity.setContentEncoding("UTF-8");
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        //httpPost.setEntity(requestEntity);

        // 响应模型
        HttpResponse response = null;
        String result = "";
        try {
            // 由客户端执行(发送)Post请求
            logger.info("请求报文:" + params);
            logger.info("开始请求————————");
            logger.info("请求中------------------");

            Long start = System.currentTimeMillis();  //开始时间
            try {
                response = httpClient.execute(httpPost);  //发送请求
            } catch (SocketTimeoutException s){
                logger.info("请求超时");
                throw new BusinessException("请求超时");
            } catch (IOException e) {
                e.printStackTrace();
            }

            Long end = System.currentTimeMillis();  //结束时间
            logger.info("请求结束————————");
            logger.info("响应时间："+ String.valueOf(end - start));

            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            System.out.println("响应状态为:" + response.getStatusLine());
            if (responseEntity != null) {
                try {
                    result = EntityUtils.toString(responseEntity,"UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("响应内容为:" + result);
            }
        }  finally {

            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }



}
