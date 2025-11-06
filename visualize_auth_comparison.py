import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# CSV files
files = ["auth_resource_cpu_1000.csv", "auth_resource_cpu_100000.csv"]

data = []
for f in files:
    df = pd.read_csv(f)
    user_count = int(f.split("_")[-1].split(".")[0])
    df["User Count"] = user_count
    # Rename types clearly
    df["Type"] = df["Type"].replace({
        "Session(Redis)": "LocalStorage",
        "HttpOnly Cookie": "HttpOnlyCookie"  # optional, just to be explicit
    })
    data.append(df)

df_all = pd.concat(data)

# Seaborn style
sns.set(style="whitegrid", palette="pastel", font_scale=1.2)

# 1️⃣ Total Wall Time
plt.figure(figsize=(10,6))
sns.barplot(data=df_all, x="User Count", y="TotalTime(ms)", hue="Type")
plt.title("Total Wall Time Comparison")
plt.ylabel("Total Wall Time (ms)")
plt.xlabel("User Count")
plt.yscale("log")
plt.legend(title="Auth Type")
plt.show()

# 2️⃣ Avg Time per Request
plt.figure(figsize=(10,6))
sns.lineplot(data=df_all, x="User Count", y="AvgTime(ns)", hue="Type", marker="o")
plt.title("Average Time per Request Comparison")
plt.ylabel("Average Time per Request (ns)")
plt.xlabel("User Count")
plt.yscale("log")
plt.grid(True, which="both", ls="--", linewidth=0.5)
plt.show()

# 3️⃣ CPU Time
plt.figure(figsize=(10,6))
sns.barplot(data=df_all, x="User Count", y="CPU(ns)", hue="Type")
plt.title("CPU Time Comparison")
plt.ylabel("CPU Time (ns)")
plt.xlabel("User Count")
plt.yscale("log")
plt.legend(title="Auth Type")
plt.show()
