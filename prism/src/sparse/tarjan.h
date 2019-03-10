#include <new>
#include <memory>
#include <stdio.h>
#include <vector>

using namespace std;
#define MAXN  9000400

int top;
vector<int> g[MAXN];
int d[MAXN], low[MAXN], scc[MAXN];
bool stacked[MAXN];
int s[MAXN];
int ticks, current_scc;
void tarjan(int u){
  d[u] = low[u] = ticks++;
  s[++top] = u; //	 s.push(u);
  stacked[u] = true;
  const vector<int> &out = g[u];
  for (int k=0, m=out.size(); k<m; ++k){
    const int &v = out[k];
    if (d[v] == -1){
      tarjan(v);
      low[u] = min(low[u], low[v]);
    }else if (stacked[v]){
      low[u] = min(low[u], low[v]);
    }
  }
  if (d[u] == low[u]){
    int v;
    do{
      v = s[top--]; //	    v = s.top();
      //s.pop();
      stacked[v] = false;
      scc[v] = current_scc;
    }while (u != v);
    current_scc++;
  }
}

