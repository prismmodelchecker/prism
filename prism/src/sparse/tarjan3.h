#include <new>
#include <memory>
#include <stdio.h>
#include <vector>

using namespace std;
#define MAXN  9000400

int top_3;
vector<int> g_3[MAXN];
int d_3[MAXN], low_3[MAXN], scc_3[MAXN];
bool stacked_3[MAXN];
int s_3[MAXN];
int ticks_3, current_scc_3;
void tarjan_3(int u){
  d_3[u] = low_3[u] = ticks_3++;
  s_3[++top_3] = u; //	 s.push(u);
  stacked_3[u] = true;
  const vector<int> &out = g_3[u];
  for (int k=0, m=out.size(); k<m; ++k){
    const int &v = out[k];
    if (d_3[v] == -1){
      tarjan_3(v);
      low_3[u] = min(low_3[u], low_3[v]);
    }else if (stacked_3[v]){
      low_3[u] = min(low_3[u], low_3[v]);
    }
  }
  if (d_3[u] == low_3[u]){
    int v;
    do{
      v = s_3[top_3--]; //	    v = s.top();
      //s.pop();
      stacked_3[v] = false;
      scc_3[v] = current_scc_3;
    }while (u != v);
    current_scc_3++;
  }
}

