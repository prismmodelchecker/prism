#include <new>
#include <memory>
#include <stdio.h>
#include <vector>

using namespace std;
#define MAXN 8200200

int top_5;
vector<int> g_5[MAXN];
int d_5[MAXN], low_5[MAXN], scc_5[MAXN];
bool stacked_5[MAXN];
int s_5[MAXN];
int ticks_5, current_scc_5;
void tarjan_5(int u){
  d_5[u] = low_5[u] = ticks_5++;
  s_5[++top_5] = u; //	 s.push(u);
  stacked_5[u] = true;
  const vector<int> &out = g_5[u];
  for (int k=0, m=out.size(); k<m; ++k){
    const int &v = out[k];
    if (d_5[v] == -1){
      tarjan_5(v);
      low_5[u] = min(low_5[u], low_5[v]);
    }else if (stacked_5[v]){
      low_5[u] = min(low_5[u], low_5[v]);
    }
  }
  if (d_5[u] == low_5[u]){
    int v;
    do{
      v = s_5[top_5--]; //	    v = s.top();
      //s.pop();
      stacked_5[v] = false;
      scc_5[v] = current_scc_5;
    }while (u != v);
    current_scc_5++;
  }
}

