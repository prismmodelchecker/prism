import pandas as pd
import matplotlib.pyplot as plt

# Load the data
df = pd.read_csv('viu.csv')


# Compute permissiveness metric

# Friendly titles for each task
title_map = {
    'ac-8x5': 'Aircraft Collision',
    'SAV-5x5': 'Semi. Autonomous Vehicle',
    'obst-v1-100': 'Frozen Lake',
    'warehouse' : 'Warehouse Navigation'
}

tasks = df['Task'].unique()
colors = ['tab:blue', 'tab:orange', 'tab:green', 'tab:red']

# Create horizontal subplots
fig, axes = plt.subplots(1, len(tasks), figsize=(15, 4), sharex=True)

for ax, task, color in zip(axes, tasks, colors):
    data = df[df['Task'] == task]
    ax.plot(data['uGap'], data['Permissiveness'], marker='o', color=color)
    ax.set_title(title_map[task], fontsize=12)
    ax.set_xlabel('u-gap')
    ax.set_ylabel('Permissiveness')
    ax.grid(axis='y', linestyle='--', alpha=0.7)

# Add a centered caption under the plots
fig.text(
    0.5,          # x = center
    0.01,         # y = 1% from bottom of figure
    'Permissiveness = 1 - #DiscardedActions / #Interger',
    ha='center',  # horizontal alignment
    va='bottom',  # vertical alignment
    fontsize='medium'
)

plt.tight_layout(rect=[0, 0.03, 1, 1])  # leave space at bottom for the caption
plt.show()
