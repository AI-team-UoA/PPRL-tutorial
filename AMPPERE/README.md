# 📚 Tutorial: [AMPPERE](https://github.com/usc-isi-i2/amppere)

> 📍 **EDBT 2026 | Tampere, Finland**
**Privacy-Preserving Record Linkage:
Past, Present and Yet-to-Come** \
L. Stetsikas, D. Karapiperis, G. Papadakis, M. Koubarakis \
Presenter: Lefteris Stetsikas 📧 istetsikas@di.uoa.gr

AMPPERE focuses on secure, privacy-preserving record linkage using Homomorphic Encryption.

## 📋 Prerequisites

Before you begin, ensure your system has the following dependencies installed:
* **CMake**
* **GCC** (C++ compiler)
* **Python** >= 3.10

---

## Step 1: Install Palisade

First, you need to install the Palisade Homomorphic Encryption library.

1. Clone the repository:

    ```bash

    git clone https://gitlab.com/palisade/palisade-release.git
    cd palisade-release

2. Build and install the library:
    ```bash
    mkdir build
    cd build
    cmake ..
    make
    sudo make install  # sudo is usually required for installation
    make testall       # Optional: verify the installation

## Step 2: Install AMPPERE

Next, download and build the AMPPERE project.

1. Clone the AMPPERE repository:
    ```bash
    git clone https://github.com/usc-isi-i2/amppere.git
    scp -r ./palisade ./ampper  #Optional for linux
    cd amppere

2. Build AMPPERE:

    ```bash
    cd palisade
    mkdir build
    cd build
    cmake ..
    make

## Step 3: Hash ABT-BUY D2 Dataset

We use the provided `febrl.py` script to encode the abt-buy dataset. This step specifically encodes the column names using n-grams and blocking. For abt-buy we used only columns `name` but any column can be used.

Run the following commands from the directory containing your python script:

```bash
cd amppere
# Encode the ABT dataset
python febrl.py ../data/D2/abtclean.csv abt_output.csv --ngram=2 --blocking --num-perm=128 --threshold=0.5
# Encode the BUY dataset
python febrl.py ../data/D2/buyclean.csv buy_output.csv --ngram=2 --blocking --num-perm=128 --threshold=0.5
```

## Step 4: Matching with Homomorphic Encryption

Move the generated abt_output.csv and buy_output.csv files into your AMPPERE build directory. You can then run the matching process in one of two modes:

### Mode A: Compare Two Random Records

This command selects two random records from the datasets and matches them.

```bash
./bgv-sr abt_output.csv buy_output.csv vr
```

Example Output:
```plaintext
Comparing record 198 from set A, and record 872 from set B
0.0357143  --> jaccard similarity
130.139    --> time in ms
```

### Mode B: Match Every Record

This command compares every record across the two datasets.

```bash

./all-comparisons abt_output.csv buy_output.csv vr
```

Example Output:
```
0.0350877
time taken (s): 347.05 ms
jaccard similarity score: 0
0.0181818
time taken (s): 323.537 ms
jaccard similarity score: 0
0.0508475
time taken (s): 342.788 ms
jaccard similarity score: 0
...

```

## 🤝 Acknowledgments

This work was supported by the [Horizon Europe](https://research-and-innovation.ec.europa.eu/funding/funding-opportunities/funding-programmes-and-open-calls/horizon-europe_en) project [Recitals](https://recitals-project.eu/index.html) (Grant No.101168490).


![alt text](/images/image.png)

