# 🔮 Prédiction de branches

Nous avons vu dans l'article précédent que les branches peuvent avoir un coût élevé en termes de performance.
Nous avons aussi vu qu'il y avait des techniques pour pouvoir éviter d'écrire ces branches.
Ces techniques ont cependant un coût très élevé en termes de complexité (de lecture).

Les CPUs modernes ont plusieurs techniques non intrusives pour réduire le coût des branches.
La plupart des développeurs peuvent simplement les ignorer et récupérer quand même une partie des bénéfices.

## Pipelines

On peut imaginer un ordinateur comme une machine très simple qui suit une séquence d'instruction et les exécute une par une, comme on le ferait pour une recette de cuisine.

Les plus vieux ordinateurs faisaient en effet cela, ils récupéraient la commande en mémoire, la décodaient, l'exécutaient, enregistraient le résultat, récupéraient la prochaine commande et répétaient jusqu'à ce qu'il n'y en n'ait plus ou que quelqu'un retire la prise.

Même si la commande C+1 n'avait pas besoin des résultats de la commande, le CPU attendait patiemment que C finisse pour exécuter C+1.

Par exemple, pour une fonction très simple :
```java
int doSomething(int a) {
    a+= 1;
    a+= 2;
    a+= 3;
    a+= 4;
    return a;
}
```

En simplifiant beaucoup, on peut considérer que l'exécution ressemblait à cela : (Fetch, Decode, Execute, Save)

|        | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 |
|--------|---|---|---|---|---|---|---|---|---|----|----|----|----|----|----|----|
| a+= 1; | F | D | E | S |   |   |   |   |   |    |    |    |    |    |    |    |
| a+= 2; |   |   |   |   | F | D | E | S |   |    |    |    |    |    |    |    |
| a+= 3; |   |   |   |   |   |   |   |   | F | D  | E  | S  |    |    |    |    |
| a+= 4; |   |   |   |   |   |   |   |   |   |    |    |    | F  | D  | E  | S  |

Les CPUs récents utilisent souvent des "Instruction Pipelines" afin d'optimiser cela.
Ces pipelines permettent par exemple d'effectuer des tâches en parallèles automatiquement.

L'exécution peut donc s'effectuer de cette manière, sans changer le résultat final :

|        | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|--------|---|---|---|---|---|---|---|
| a+= 1; | F | D | E | S |   |   |   |
| a+= 2; |   | F | D | E | S |   |   |
| a+= 3; |   |   | F | D | E | S |   |
| a+= 4; |   |   |   | F | D | E | S |

En supposant que les temps F, D, E et S sont équivalents : on aurait rendu le programme ~2.2 (16/7) fois plus rapide !

Si une branche conditionnelle est présente, alors on peut l'imaginer comme un train devant son aiguillage.
![Trolley](trolley.jpg "Example de branche")

Le CPU ne peut pas savoir avec certitude quelle sera la prochaine tâche à effectuer.

Par exemple, en reprenant le même programme, mais en ajoutant une condition :
```java
int doSomething(int a) {
    a+= 1;
    a+= 2;
    if (a < 100) {
      a+= 3;
    }
    a+= 4;
    return a;
}
```

L'exécution ressemblerait à ça, si la branche est prise :

|              | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|--------------|---|---|---|---|---|---|---|---|---|----|----|
| a+= 1;       | F | D | E | S |   |   |   |   |   |    |    |
| a+= 2;       |   | F | D | E | S |   |   |   |   |    |    |
| if (a < 100) |   |   | F | D | E | S |   |   |   |    |    |
| a+= 3;       |   |   |   |   |   |   | F | D | E | S  |    |
| a+= 4;       |   |   |   |   |   |   |   | F | D | E  | S  |

ou si la branche n'est pas prise :

|              | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|--------------|---|---|---|---|---|---|---|---|---|----|
| a+= 1;       | F | D | E | S |   |   |   |   |   |    |
| a+= 2;       |   | F | D | E | S |   |   |   |   |    |
| if (a < 100) |   |   | F | D | E | S |   |   |   |    |
| a+= 3;       |   |   |   |   |   |   |   |   |   |    |
| a+= 4;       |   |   |   |   |   |   | F | D | E | S  |

On peut voir assez simplement que l'impact d'une branche sur le temps de traitement n'est pas négligeable !
Heureusement, il existe une technique qui permet de réduire l'impact de la branche sur le nombre de cycles du CPU : la prédiction de branche.


Quand le CPU tombe sur une branche conditionnelle, il examine les probabilités de prendre tel ou tel chemin (en fonction des cas déjà rencontré).
Il va "considérer" que cette branche est vraie et va continuer l'exécution telle quelle.

Par exemple, si la branche est prédite comme prise et l'est effectivement, l'exécution ressemblerait à :

|              | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|--------------|---|---|---|---|---|---|---|---|---|
| a+= 1;       | F | D | E | S |   |   |   |   |   |
| a+= 2;       |   | F | D | E | S |   |   |   |   |
| if (a < 100) |   |   | F | D | E | S |   |   |   |
| a+= 3;       |   |   |   | F | D |   | E | S |   |
| a+= 4;       |   |   |   |   | F | D |   | E | S |

On gagne deux cycles, soit ~19% (9/11) plus rapide que sans cette optimisation.

Si par contre, la branche est prédite comme prise, mais le prédicteur s'est trompé, il devra laisser tomber ce qu'il était en train de faire et recommencer à la bonne branche.

L'exécution ressemble à ça quand la prédiction est mauvaise :

|              | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|--------------|---|---|---|---|---|---|---|---|---|----|
| a+= 1;       | F | D | E | S |   |   |   |   |   |    |
| a+= 2;       |   | F | D | E | S |   |   |   |   |    |
| if (a < 100) |   |   | F | D | E | S |   |   |   |    |
| a+= 3;       |   |   |   | F | D |   |   |   |   |    |
| a+= 4;       |   |   |   |   |   |   | F | D | E | S  |

Quand le prédicteur se trompe, on est plus lent que quand il a raison, mais le temps d'exécution n'est pas plus mauvais que s'il n'y avait pas de prédicteur du tout.

La prédiction de branche peut donc nous faire gagner quelques cycles... Est-ce négligeable en pratique ou peut-on voir l'effet sur du vrai code en production ?

### Exemple

Un exemple assez connu viens de la question la [plus upvotée de Stack Overflow](https://stackoverflow.com/questions/11227809/why-is-processing-a-sorted-array-faster-than-processing-an-unsorted-array).

On part d'un tableau d'entier qui contient des nombres aléatoire compris entre `[-1000; 1000]` et on écrit un programme qui additionne tout les entiers positifs de ce tableau.

En Java, on pourrait écrire le programme de cette manière :
```java
    int[] array = IntStream.generate(() -> rnd.nextInt() % 1_000)
        .limit(30_000)
        .toArray();
    int[] sortedArray = IntStream.of(input).sorted().toArray();

    long sumPositive(int[] array) {
        long sum = 0;
        for (int value : array) {
          if (value >= 0) {
            sum += value;
          }
        }
        return sum;
    }
```


L'appel de la fonction `sumPositive` fait exactement le même nombre de calculs qu'on lui passe le tableau trié ou non.
Cependant, la version triée va environ 5 fois plus vite.

| Benchmark               | Score                       |
|-------------------------|-----------------------------|
| sortedArray             | 104995.716 ± 473.420  ops/s |
| unsortedArray           | 22568.343 ± 102.114  ops/s  |

On peut voir le même effet en utilisant les APIs `java.util.Stream` :

```java
    private long sumStream(int[] array) {
      return Arrays.stream(array).filter(i -> i >= 0).sum();
    }
```

| Benchmark               | Score                       |
|-------------------------|-----------------------------|
| streamSortedArray       | 97041.833 ± 610.366  ops/s  |
| streamUnsortedArray     | 21495.011 ± 145.442  ops/s  |

La version "branchless" n'est pas impactée par le tri du tableau.

```java
    private long branchlessSumPositive(int[] array) {
      long sum = 0;
      for (int value : array) {
        sum += ~(value >> 31) & value;
      }
      return sum;
    }
```

| Benchmark               | Score                       |
|-------------------------|-----------------------------|
| branchlessSortedArray   | 70806.141 ± 260.635  ops/s  |
| branchlessUnsortedArray | 70930.320 ± 354.355  ops/s  |
