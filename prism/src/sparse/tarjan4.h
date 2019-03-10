#include <new>
#include <memory>
#include <stdio.h>
#include <vector>

using namespace std;
#define MAXN  9000400

int top_4;
vector<int> g_4[MAXN];
int d_4[MAXN], low_4[MAXN], scc_4[MAXN];
bool stacked_4[MAXN];
int s_4[MAXN];
int ticks_4, current_scc_4;
void tarjan_4(int u){
  d_4[u] = low_4[u] = ticks_4++;
  s_4[++top_4] = u; //	 s.push(u);
  stacked_4[u] = true;
  const vector<int> &out = g_4[u];
  for (int k=0, m=out.size(); k<m; ++k){
    const int &v = out[k];
    if (d_4[v] == -1){
      tarjan_4(v);
      low_4[u] = min(low_4[u], low_4[v]);
    }else if (stacked_4[v]){
      low_4[u] = min(low_4[u], low_4[v]);
    }
  }
  if (d_4[u] == low_4[u]){
    int v;
    do{
      v = s_4[top_4--]; //	    v = s.top();
      //s.pop();
      stacked_4[v] = false;
      scc_4[v] = current_scc_4;
    }while (u != v);
    current_scc_4++;
  }
}

