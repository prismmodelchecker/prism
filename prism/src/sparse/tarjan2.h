#include <new>
#include <memory>
#include <stdio.h>
#include <vector>

using namespace std;
#define MAXN  9000400

int top_2;
vector<int> g_2[MAXN];
int d_2[MAXN], low_2[MAXN], scc_2[MAXN];
bool stacked_2[MAXN];
int s_2[MAXN];
int ticks_2, current_scc_2;
void tarjan_2(int u){
  d_2[u] = low_2[u] = ticks_2++;
  s_2[++top_2] = u; //	 s.push(u);
  stacked_2[u] = true;
  const vector<int> &out = g_2[u];
  for (int k=0, m=out.size(); k<m; ++k){
    const int &v = out[k];
    if (d_2[v] == -1){
      tarjan_2(v);
      low_2[u] = min(low_2[u], low_2[v]);
    }else if (stacked_2[v]){
      low_2[u] = min(low_2[u], low_2[v]);
    }
  }
  if (d_2[u] == low_2[u]){
    int v;
    do{
      v = s_2[top_2--]; //	    v = s.top();
      //s.pop();
      stacked_2[v] = false;
      scc_2[v] = current_scc_2;
    }while (u != v);
    current_scc_2++;
  }
}

