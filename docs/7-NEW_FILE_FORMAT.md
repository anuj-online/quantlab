We need to update BhavcopyLoaderService.java to handle the new CSV file header format.

Update are going to be mostly in this method

```java 
private BhavcopyRow parseCsvLine(String line)
```


existing file headers are
`SYMBOL, SERIES, DATE1, PREV_CLOSE, OPEN_PRICE, HIGH_PRICE, LOW_PRICE, LAST_PRICE, CLOSE_PRICE, AVG_PRICE, TTL_TRD_QNTY, TURNOVER_LACS, NO_OF_TRADES, DELIV_QTY, DELIV_PER`

new file headers will be 

`SYMBOL,SERIES,OPEN,HIGH,LOW,CLOSE,LAST,PREVCLOSE,TOTTRDQTY,TOTTRDVAL,TIMESTAMP,TOTALTRADES,ISIN`

some columns have moved position and some columns have been renamed. We need to map them correctly to the BhavcopyRow object.

total number of columns have been reduced from 15 to 13.

Change in columns:
DATE1 -> TIMESTAMP ( change in name and position)
OPEN_PRICE -> OPEN ( change in name and position)
HIGH_PRICE -> HIGH ( change in name and position)
LOW_PRICE -> LOW ( change in name and position)
CLOSE_PRICE -> CLOSE ( change in name and position)
LAST_PRICE -> LAST ( change in name and position)
TTL_TRD_QNTY -> TOTTRDQTY ( change in name and position)
TURNOVER_LACS -> TOTTRDVAL ( change in name and position)
NO_OF_TRADES -> TOTALTRADES ( change in name and position)
PREV_CLOSE -> PREVCLOSE ( change in position)


other fields are gone missing which are already ignored in the model.

No changes in database model, we still have all the content we need. 

Only update the parsing logic to map the new columns correctly.

Providing demo file content for testing
```csv
SYMBOL,SERIES,OPEN,HIGH,LOW,CLOSE,LAST,PREVCLOSE,TOTTRDQTY,TOTTRDVAL,TIMESTAMP,TOTALTRADES,ISIN
SGBFEB32IV,GB,16240.2,17513,15100,17434.31,17460,15921.74,7874,131376600.92,03-Feb-2026,1445,IN0020230184
20MICRONS,EQ,178.8,178.8,170.4,172.88,172.2,168.86,108007,18752311.37,03-Feb-2026,3683,INE144J01027
```