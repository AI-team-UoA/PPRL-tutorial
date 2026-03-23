# 📚 Tutorial: [PRIMAT](https://git.informatik.uni-leipzig.de/dbs/pprl/primat)

> 📍 **EDBT 2026 | Tampere, Finland**
**Privacy-Preserving Record Linkage:
Past, Present and Yet-to-Come** \
L. Stetsikas, D. Karapiperis, G. Papadakis, M. Koubarakis \
Presenter: Lefteris Stetsikas 📧 istetsikas@di.uoa.gr


# 📋 Prerequisites

* ☕ **Java:** Version 11
* 📦 **Maven:** Installed and configured


## Primat Tutorial: Data Owner Guide

This guide outlines the steps for a Data Owner to prepare and encode datasets using Primat.

### Step 0: Prepare Your Dataset

Each dataset you want to encode should contain these columns:

* `id`: Unique id for each record
* `global_id`: If a set of records share the same `global_id`, then they reffer to the same entity. (Optional, instead of ground-truth)
* `party`: Dataowner's id (e.g : "abt", "buy")

👉 **[Open Notebook to make abt-buy work with PRIMAT](data/data_to_fit_primat.ipynb)**


### Step 1 : Preprocess Data

Before running the preprocessing commands, you must open @Preprocessing.java and explicitly specify the correct columns for your datasets.

Once configured, run the following commands to preprocess your data:

👉 **[Check Preprocessing Method](src/main/java/com/example/Preprocessing.java)**


```bash
mvn clean compile exec:java -Dexec.mainClass="Preprocessing" -Dexec.args="./data/abt_primat.csv abt.csv"

mvn clean compile exec:java -Dexec.mainClass="Preprocessing" -Dexec.args="./data/buy_primat.csv buy.csv"
```

### Step 2: Encode Data

Next, encode the preprocessed datasets:

👉 **[Check Encoding Method](src/main/java/com/example/Encoding.java)**

```bash
mvn clean compile exec:java -Dexec.mainClass="Encoding" -Dexec.args="abt.csv abt_enc.csv"
mvn clean compile exec:java -Dexec.mainClass="Encoding" -Dexec.args="buy.csv buy_enc.csv"
```

### Step 3: Block Data (Optional)

Note: This step is optional but recommended for optimizing the matching process.

Run the blocking algorithm on your encoded datasets:

👉 **[Check Blocking Method](src/main/java/com/example/Blocking.java)**

```bash
mvn clean compile exec:java -Dexec.mainClass="Blocking" -Dexec.args="abt_enc.csv abt_blocks.csv"
mvn clean compile exec:java -Dexec.mainClass="Blocking" -Dexec.args="buy_enc.csv buy_blocks.csv"
```


## Third-Party Steps

### Step 4: Matching

Finally, run the batch matching process. (In the example below, 0.6 represents the similarity threshold, and 1064 is the total of our actual matches, for the evaluation metrics):

👉 **[Check Linking Method](src/main/java/com/example/BatchMatching.java)**


```Bash

mvn clean compile exec:java -Dexec.mainClass="BatchMatching" -Dexec.args="abt_enc.csv buy_enc.csv 0.6 1064
```

This work was supported by the [Horizon Europe](https://research-and-innovation.ec.europa.eu/funding/funding-opportunities/funding-programmes-and-open-calls/horizon-europe_en) project [Recitals](https://recitals-project.eu/index.html) (Grant No.101168490).


![alt text](/images/image.png)