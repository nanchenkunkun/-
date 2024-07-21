# 典型回答
在回答这个问题之前，我们先看一段用JDBC和Mybatis写的代码：
```java
public void testJdbc() {
    String url = "dbLink";
    try(Connection conn = DriverManager.getConnection(url, "root", "password")){
        // 加载MySQL驱动
        Class.forName("com.mysql.cj.jdbc.Driver");
        int author = 1;
        String date = "2018.06.10";
        String sql = "SELECT id, title, content, create_time FROM article WHERE author_id = " + author
                + " AND create_time > '" + date + "'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Article> articles = new ArrayList<>(rs.getRow());
        while (rs.next()) {
            Article article = new Article();
            article.setId(rs.getInt("id"));
            article.setTitle(rs.getString("title"));
            article.setContent(rs.getString("content"));
            article.setCreateTime(rs.getDate("create_time"));
            articles.add(article);
        }
        System.out.println("Query SQL ==> " + sql);
        System.out.println("Query Result: ");
        articles.forEach(System.out::println);
    } catch (ClassNotFoundException | SQLException e) {
        e.printStackTrace();
    }
}
```
```java
public void testMyBatis() throws IOException {
	String resource = "mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    SqlSessionFactory sqlSessionFactory = new
            SqlSessionFactoryBuilder().build(inputStream);
    inputStream.close();
    try(SqlSession session = sqlSessionFactory.openSession()) {
        ArticleDao articleDao = session.getMapper(ArticleDao.class);
        List<Article> articles = articleDao.
                findByAuthorAndCreateTime(1, "2018-06-10");
        System.out.println(articles);
    }
}
inteface ArticleDao {
    @Query({"SELECT id, title, content, create_time FROM article WHERE author_id = #{id} AND create_time > #{time}"})
	List<Article> findByAuthorAndCreateTime(int id, String time);
}
```
很明显，我们能看到使用Mybatis的代码，结构更清晰，代码量也比较少，这就是Mybatis最直观的优点：

1. 将数据库的操作逻辑和业务操作**解耦合**，使得开发人员可以专心业务逻辑的处理。
2. 开发人员只写Sql就可以访问数据库，**不需要关心各种数据库连接等额外的操作**。各种Connection和Statement都交给了Mybatis来管理
3. 可以**将数据库表的字段按照业务规则直接映射到DO层**，不用再像JDBC一样需要业务代码来转换

除此之外，还有其他优点：

4. **支持多种数据源**，如POOLED，UNPOOLED，JNDI。同时，还可以整合其他数据库连接池如HikariCP，Druid，C3p0等
5. **支持动态SQL**，大大减少了代码的开发量，如if/foreach等常用的动态标签
6. **支持事务性的一级缓存，二级缓存和自定义缓存**，其中，一级缓存是以session为生命周期，默认开启；二级缓存则是根据配置的算法来计算过期时间（FIFO，LRU等），二级缓存如果操作不当容易产生脏数据，不建议使用
