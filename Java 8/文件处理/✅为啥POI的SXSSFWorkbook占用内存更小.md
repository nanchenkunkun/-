# 典型回答

SXSSFWorkbook 类是为了处理大型 Excel 文件而设计的。它的实现原理是**通过将部分数据写入磁盘上的临时文件来减少内存占用**。

在SXSSFWorkbook类中，有一个类叫做SheetDataWriter，这个类的作用就是将部分数据写入磁盘上的临时文件的。

```java
public class SXSSFWorkbook implements Workbook {
	protected SheetDataWriter createSheetDataWriter() throws IOException {
        if(_compressTmpFiles) {
            return new GZIPSheetDataWriter(_sharedStringSource);
        }
        
        return new SheetDataWriter(_sharedStringSource);
    }
}
```

写入过程是在 SheetDataWriter 的 writeRow 方法中实现的。此方法会被 SXSSFSheet 调用，以将行数据转换成 XML 并写入临时文件。

```java
public void writeRow(int rownum, SXSSFRow row) throws IOException {
    if (_numberOfFlushedRows == 0)
        _lowestIndexOfFlushedRows = rownum;
    _numberLastFlushedRow = Math.max(rownum, _numberLastFlushedRow);
    _numberOfCellsOfLastFlushedRow = row.getLastCellNum();
    _numberOfFlushedRows++;
    beginRow(rownum, row);
    Iterator<Cell> cells = row.allCellsIterator();
    int columnIndex = 0;
    while (cells.hasNext()) {
        writeCell(columnIndex++, cells.next());
    }
    endRow();
}
```

writeRow方法会循环调用writeCell方法：

```java
public void writeCell(int columnIndex, Cell cell) throws IOException {
    if (cell == null) {
        return;
    }
    String ref = new CellReference(_rownum, columnIndex).formatAsString();
    _out.write("<c");
    writeAttribute("r", ref);
    CellStyle cellStyle = cell.getCellStyle();
    if (cellStyle.getIndex() != 0) {
        // need to convert the short to unsigned short as the indexes can be up to 64k
        // ideally we would use int for this index, but that would need changes to some more
        // APIs
        writeAttribute("s", Integer.toString(cellStyle.getIndex() & 0xffff));
    }
    CellType cellType = cell.getCellType();
    switch (cellType) {
        case BLANK: {
            _out.write('>');
            break;
        }
        case FORMULA: {
            switch(cell.getCachedFormulaResultType()) {
                case NUMERIC:
                    writeAttribute("t", "n");
                    break;
                case STRING:
                    writeAttribute("t", STCellType.STR.toString());
                    break;
                case BOOLEAN:
                    writeAttribute("t", "b");
                    break;
                case ERROR:
                    writeAttribute("t", "e");
                    break;
            }
            _out.write("><f>");
            outputQuotedString(cell.getCellFormula());
            _out.write("</f>");
            switch (cell.getCachedFormulaResultType()) {
                case NUMERIC:
                    double nval = cell.getNumericCellValue();
                    if (!Double.isNaN(nval)) {
                        _out.write("<v>");
                        _out.write(Double.toString(nval));
                        _out.write("</v>");
                    }
                    break;
                case STRING:
                    String value = cell.getStringCellValue();
                    if(value != null && !value.isEmpty()) {
                        _out.write("<v>");
                        _out.write(value);
                        _out.write("</v>");
                    }
                    break;
                case BOOLEAN:
                    _out.write("><v>");
                    _out.write(cell.getBooleanCellValue() ? "1" : "0");
                    _out.write("</v>");
                    break;
                case ERROR: {
                    FormulaError error = FormulaError.forInt(cell.getErrorCellValue());

                    _out.write("><v>");
                    _out.write(error.getString());
                    _out.write("</v>");
                    break;
                }
            }
            break;
        }
        case STRING: {
            if (_sharedStringSource != null) {
                XSSFRichTextString rt = new XSSFRichTextString(cell.getStringCellValue());
                int sRef = _sharedStringSource.addSharedStringItem(rt);

                writeAttribute("t", STCellType.S.toString());
                _out.write("><v>");
                _out.write(String.valueOf(sRef));
                _out.write("</v>");
            } else {
                writeAttribute("t", "inlineStr");
                _out.write("><is><t");
                if (hasLeadingTrailingSpaces(cell.getStringCellValue())) {
                    writeAttribute("xml:space", "preserve");
                }
                _out.write(">");
                outputQuotedString(cell.getStringCellValue());
                _out.write("</t></is>");
            }
            break;
        }
        case NUMERIC: {
            writeAttribute("t", "n");
            _out.write("><v>");
            _out.write(Double.toString(cell.getNumericCellValue()));
            _out.write("</v>");
            break;
        }
        case BOOLEAN: {
            writeAttribute("t", "b");
            _out.write("><v>");
            _out.write(cell.getBooleanCellValue() ? "1" : "0");
            _out.write("</v>");
            break;
        }
        case ERROR: {
            FormulaError error = FormulaError.forInt(cell.getErrorCellValue());

            writeAttribute("t", "e");
            _out.write("><v>");
            _out.write(error.getString());
            _out.write("</v>");
            break;
        }
        default: {
            throw new IllegalStateException("Invalid cell type: " + cellType);
        }
    }
    _out.write("</c>");
}
```

在这个方法中，数据会在 _out.write(...) 调用时写入磁盘，这里的_out其实就是一个写入磁盘文件的Writer，他的write方法就会把内容写入到临时文件中。

我尝试着在_out初始化的地方，也就是：

```java
public SheetDataWriter() throws IOException {
    _fd = createTempFile();
    _out = createWriter(_fd);
}
```

中加了断点，就能在运行过程中找到这个临时文件，tail一下临时文件就会发现他不断地在有文件写入。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1700376924337-169f9616-ab5a-4ec6-a94a-0123000536ec.png#averageHue=%232d2828&clientId=u408dc7f3-c628-4&from=paste&height=123&id=u3d68a4b6&originHeight=123&originWidth=1264&originalType=binary&ratio=1&rotation=0&showTitle=false&size=119487&status=done&style=none&taskId=u4b4b2dbe-a513-4dc2-b0ed-6ec6b1b0b3e&title=&width=1264)

感兴趣的也可以debug看一下这个临时文件的内容，其实他就是一个xml文件，然后写入的就是我们excel中的内容。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1700376941273-908b7369-ec83-4c16-a951-3754d41897a0.png#averageHue=%233a3a3a&clientId=u408dc7f3-c628-4&from=paste&height=48&id=u758480f5&originHeight=48&originWidth=955&originalType=binary&ratio=1&rotation=0&showTitle=false&size=40726&status=done&style=none&taskId=u23bb6a95-28d6-4a8b-9c71-e73009b7f4f&title=&width=955)

**所以，在SXSSFWorkbook中，我们在写入文件时，并不是把所有内容都暂留在内存内，而是会把部分数据写入临时文件，来减少对内存的占用，内存中只保留当前的一部分数据，这样就可以避免内存溢出的问题了、**

# 扩展知识

## 配置行缓存限制

我们可以主动设置行缓存限制，超过这个限制的数据将被写入磁盘上的临时文件。在创建SXSSFWorkbook的时候，可以指定rowAccessWindowSize来实现。

```
  /**
   * Construct an empty workbook and specify the window for row access.
   * <p>
   * When a new node is created via {@link SXSSFSheet#createRow} and the total number
   * of unflushed records would exceed the specified value, then the
   * row with the lowest index value is flushed and cannot be accessed
   * via {@link SXSSFSheet#getRow} anymore.
   * </p>
   * <p>
   * A value of <code>-1</code> indicates unlimited access. In this case all
   * records that have not been flushed by a call to <code>flush()</code> are available
   * for random access.
   * </p>
   * <p>
   * A value of <code>0</code> is not allowed because it would flush any newly created row
   * without having a chance to specify any cells.
   * </p>
   *
   * @param rowAccessWindowSize the number of rows that are kept in memory until flushed out, see above.
   */
  public SXSSFWorkbook(int rowAccessWindowSize){
    this(null /*workbook*/, rowAccessWindowSize);
  }

```
