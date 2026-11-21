# GUI regression: model constant defaults after reload

Manual check. There is no Swing harness; `make -C prism tests` and the CLI do not open this dialog.

Save this model. On the Properties tab, add and select `P=? [ F s=N ]`.

```
dtmc

const int N;

module m
	s : [0..N] init 0;
	[] s<N -> (s'=s+1);
	[] s=N -> (s'=s);
endmodule
```

1. Properties | Verify. `N` (type `int`) has an empty Value. Enter `3` and click Okay. Result: 1.
2. Model | Reload model.
3. Properties | Verify. Value must be `3`. Click Okay. Result: 1.
