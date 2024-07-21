# 典型回答

在 MySQL 中，有的时候我们想要知道一个 SQL 的执行耗时有多少，有些数据库管理工具客户端是可以直接返回一个执行时长的，但是，如果没有这种工具怎么办呢？如何查看一个 SQL 的耗时情况呢？

有两种办法，如果是 MySQL 8.0之前，可以用SHOW PROFILES、如果是 MySQL 8.0之后的版本，可以用EXPLAIN ANALYZE

### SHOW PROFILES
<br />SHOW PROFILES 是 MySQL 提供的一种用于查看查询执行概要信息的命令。它能够显示当前会话中最近执行的 SQL 语句的性能数据，包括每条语句的执行时间和详细的执行阶段耗时。

想要使用这个功能，需要先开启profiling：

```
-- 启用 Profiling
SET profiling = 1;
```

启动之后，执行你自己的 SQL 即可：

```
-- 执行查询
select * from t1;
```

之后，通过SHOW PROFILES查询概要信息：

```
-- 查看 Profiles
SHOW PROFILES;

+----------+------------+------------------+                                                                                                                 
| Query_ID | Duration   | Query            |                                                                                                                 
+----------+------------+------------------+                                                                                                                 
|        1 | 0.00050800 | select * from t1 |                                                                                                                 
+----------+------------+------------------+                                                                                                                 
1 row in set, 1 warning (0.00 sec)   

```

这里面显示了刚刚我们执行的SQL的执行情况，耗时是0.00050800秒。

如果我们的想要查询具体某个 SQL 的耗时详细信息，也可以通过`SHOW PROFILE FOR QUERY` 命令：

```
-- 查看特定查询的详细信息
SHOW PROFILE FOR QUERY 1;

+--------------------------------+----------+                                                                                                                
| Status                         | Duration |                                                                                                                
+--------------------------------+----------+                                                                                                                
| starting                       | 0.000124 |                                                                                                                
| Executing hook on transaction  | 0.000008 |                                                                                                                
| starting                       | 0.000013 |                                                                                                                
| checking permissions           | 0.000017 |                                                                                                                
| Opening tables                 | 0.000058 |                                                                                                                
| init                           | 0.000010 |                                                                                                                
| System lock                    | 0.000017 |                                                                                                                
| optimizing                     | 0.000009 |                                                                                                                
| statistics                     | 0.000030 |                                                                                                                
| preparing                      | 0.000036 |                                                                                                                
| executing                      | 0.000076 |                                                                                                                
| end                            | 0.000007 |                                                                                                                
| query end                      | 0.000006 |                                                                                                                
| waiting for handler commit     | 0.000017 |                                                                                                                
| closing tables                 | 0.000016 |                                                                                                                
| freeing items                  | 0.000044 |                                                                                                                
| cleaning up                    | 0.000022 |                                                                                                                
+--------------------------------+----------+                                                                                                                
17 rows in set, 1 warning (0.00 sec) 

```

这里详细列出了每个阶段（Status）花费的时长。可以帮助我们进行 SQL 的详细分析，其中比较重要的是以下几个：

1. Opening tables：
   - 如果在这个阶段花费的时间较长，可能意味着表的打开操作比较耗时，可能需要检查表的状态、文件系统性能等。
2. optimizing：
   - 这是查询优化的关键阶段。如果在此花费了较多时间，可能需要检查查询的复杂度和索引的使用情况。
3. statistics：
   - 这个阶段涉及收集表和索引的统计信息。如果时间过长，可能需要检查表的统计信息是否需要更新。
4. Sending data：
   - 这是查询执行的核心阶段，包含数据的读取和处理。如果在这里花费了大量时间，通常需要检查查询的效率、数据量和网络传输性能等。

### **EXPLAIN ANALYZE**
<br />EXPLAIN ANALYZE 是 MySQL 8.0.18 引入的一种功能，它结合了 EXPLAIN 和实际执行查询的分析结果，可以为每个执行计划的步骤提供真实的执行时间和行数。使用 EXPLAIN ANALYZE 可以帮助更准确地了解查询的性能瓶颈，并进行优化。

```
explain analyze select * from t1;                                                                                                                     
+-----------------------------------------------------------------------+         
| EXPLAIN                                                               |         
+-----------------------------------------------------------------------+         
| -> Table scan on t1  (cost=0.35 rows=1) (actual time=0.042..0.048 rows=1 loops=1)                                                                      |         
+-----------------------------------------------------------------------+                                                                       
1 row in set (0.00 sec)                                                                                                                                         
```

这里的(actual time=0.042..0.048 rows=1 loops=1)，表示实际执行时间和行数。actual time 显示了执行这个步骤的时间范围，rows 显示实际扫描的行数，loops 显示执行这个步骤的次数。
