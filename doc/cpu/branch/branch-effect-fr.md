# Les `if` sont chers !

Les branches de code (`if`, `switch`, `goto`, appels de fonctions, etc) peuvent avoir un coût non négligeable.
Pour comprendre pourquoi, nous devons inspecter le hardware : les programmes sont constitués d'une suite d'instruction. L'instruction courante est enregistré dans quelque chose qui est appelé généralement le "program counter" (PC).

En fonction de l'architecture du CPU, les instructions peuvent être à taille fixe (par exemple RISC) ou plus généralement à taille variable. 
Pour simplifier, on peut imaginer que le mécanisme qui décode les instructions doive trouver la taille de l'instruction courante, puis il peut commencer à lire la suivante.

Par contre, pour les instructions dites "branch" : trouver l'instruction suivante est légèrement plus compliqué que simplement lire juste après la courante.
Les branches sont des "goto" : elles indiquent au processeur où se trouve la prochaine instruction à exécuter.

Les branches peuvent être conditionnelles, ou non, et la cible peut être fixée ou calculée.
Les branches conditionnelles (typiquement `if`, `for`, `while`, etc.) sont faciles à comprendre : une branche conditionnelle n'est prise que si une certaine condition est vraie (par exemple, `a == b`).
Si la branche n'est pas prise, alors le CPU lira l'instruction suivante comme d'habitude.

Les branches non conditionnelles sont toujours prises. On les trouve souvent dans des boucles infinies, des appels de fonction, des retours de fonctions, des `break`, `continue`, `goto` etc

Les branches conditionnelles peuvent avoir un coût impressionnant, car elles empêchent plusieurs niveaux (compilateur/JIT/CPU/etc) d'optimiser automatiquement le code courant, par example :
- le compilateur ne peut pas vectoriser automatiquement et utiliser des instructions type SIMD (Single instruction, multiple data): [~3 à 12 fois plus rapide](https://stackoverflow.blog/2020/07/08/improving-performance-with-simd-intrinsics-in-three-use-cases/)
- le JIT ne peut pas inliner [10-40% plus rapide](https://www.cs.cmu.edu/~745/papers/p134-ayers.pdf)
- le CPU ne peut pas exécuter en [out-of-order](https://en.wikipedia.org/wiki/Out-of-order_execution) 

## Exemple

Afin de voir l'effet d'une branche, et les techniques typiques pour les retirer, nous allons écrire une fonction `toUppercase` qui prend 
une chaîne de caractères en entrée et la ressort en majuscule.
Cette fonction transformera `Hello, World !` en `HELLO, WOLRD !`.

Pour simplifier, nous n'allons convertir que l'alphabet latin `[a~z]` et ignorer les cas aux limites suivant la langue de l'utilisateur.

Nous allons utiliser la table ASCII afin de mettre en majuscule. En Java, une String est juste un tableau de char et chaque char représente un caractère ASCII:

| char | valeur|
|------|-------|
| @    | 64    |
| A    | 65    |
| B    | 66    |
| …    | …     |
| Z    | 90    |
| [    | 91    |
| …    | …     |
| `    | 96    |
| a    | 97    |
| b    | 98    |
| …    | …     |
| z    | 122   | 
| {    | 123   | 


`Hello` est en fait: `[72, 101, 108, 108, 111]`

On peut convertir de `a` à `A` en retirant`97-65=32`, et de même, `z` vers `Z` en retirant aussi `32`.
Par contre, nous ne devons pas toucher au caractère hors de la plage `[97, 122]` (`[a~z]`): `[` n'est pas un `{` majuscule.

Une version basique de cette fonction peut s'écrire comme ça :
```java
  public String branchToUppercase(String input) {
    char[] chars = input.toCharArray();

    for (int i = 0; i < chars.length; i++) {
      if (chars[i] >= 97 && chars[i] <= 122) {
        chars[i] -= 32;
      }
    }

    return String.valueOf(chars);
  }
```

L'astuce typique pour transformer du code avec branche en code "branchless" est d'utiliser un masque binaire pour ignorer certaines parties d'une équation.

On peut remplacer cette partie : 
```java
      if (chars[i] >= 97 && chars[i] <= 122) {
        chars[i] -= 32;
      }
```

par:
```java
      chars[i] -= (((96 - chars[i]) & (chars[i] - 123)) >> 31) & 32;
```

Qui est beaucoup moins lisible ! N'écrivez pas ce genre de code si vous n'avez pas la preuve qu'il est indispensable à rendre votre programme viable.
 
Pour la comprendre, lisons la bout à bout :

`int customEquation = ((96 - chars[i]) & (chars[i] - 123))`

Cette équation est écrite pour être positive ou égale à 0 pour tous les nombres hors de `[97; 122]`

Ensuite, nous utilisons une propriété des `int`: le bit le plus haut est le bit de signe.
S'il est égal à 1, cela veut dire que ce nombre est négatif.

On peut récupérer ce bit via un décalage de 31.

`(((96 - chars[i]) & (chars[i] - 123)) >> 31)` sera égal à `-1` si on est dans `[a~z]` (sinon 0)

`(((96 - chars[i]) & (chars[i] - 123)) >> 31) & 32` sera égal `32` si on est dans `[a~z]` (sinon 0)

Cela nous permet de faire des comparaisons de valeur sans avoir besoin de `if` ou autres branches.

En lançant ces deux méthodes dans JMH, j'obtiens sur ma machine ces résultats :

| Benchmark             | Mode  | Cnt | Score    | Error     | Units |
|-----------------------|-------|-----|----------|-----------|-------|
| branchToUppercase     | thrpt | 3   | 1710.388 | ± 110.290 | ops/s |
| branchlessToUppercase | thrpt | 3   | 5600.499 | ± 218.414 | ops/s |

Environ ~3.3 fois plus rapide ! 

> Attention : prenez soin de profiler sur la machine cible. Les ordinateurs modernes sont extrêmement complexes : l'architecture du CPU (ARM/x86/etc), le vendeur (Intel/AMD/IBM/etc), la version du compiler/OS/kernel, les accès mémoires, le cycle lunaire... Tout peut avoir un impact énorme (ou pas du tout).
