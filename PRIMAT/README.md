# Tutorial on Primat

Prerequisites:

Java 11.0
maven


## Dataowner's Steps


## Step 0 - Dataset to fit Primat

Dataset should contain id and party columns

In my example i have combined them in one and then concatenated them



## Step 1  - Preprocess Data

@ Preprocessing.java you need to specify correctly the columns

Run `mvn clean compile exec:java -Dexec.mainClass="Preprocessing" -Dexec.args="./data/abt_primat.csv abt.csv"`
Run `mvn clean compile exec:java -Dexec.mainClass="Preprocessing" -Dexec.args="./data/buy_primat.csv buy.csv"`


## Step 2 - Encode Data

`mvn clean compile exec:java -Dexec.mainClass="Encoding" -Dexec.args="abt.csv abt_enc.csv"`
`mvn clean compile exec:java -Dexec.mainClass="Encoding" -Dexec.args="buy.csv buy_enc.csv"`

## Step 3 - Blocking Data (Optional)

`mvn clean compile exec:java -Dexec.mainClass="Blocking" -Dexec.args="abt_enc.csv abt_blocks.csv"`
`mvn clean compile exec:java -Dexec.mainClass="Encoding" -Dexec.args="buy_enc.csv buy_blocks.csv"`


## Step 4 - Matching

`mvn clean compile exec:java -Dexec.mainClass="BatchMatching" -Dexec.args="abt_enc.csv buy_enc.csv 0.6 1064"`


