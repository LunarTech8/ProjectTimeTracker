# Coding Style Guidelines

## Formatting Rules

✅ **No empty lines within functions** - Functions should be compact with no blank lines inside

✅ **Exactly ONE empty line between functions** - Consistent spacing between method declarations

✅ **Opening braces on new line** - Use Allman brace style
```java
// Good:
private void priorityAddAll(Set<ArticleEntity> prioritySet, final List<ArticleEntity> articles)
{
    articles.forEach((var article) -> article.setHasPriority(true));
    prioritySet.addAll(articles);
}

// Bad:
private void priorityAddAll(Set<ArticleEntity> prioritySet, final List<ArticleEntity> articles) {
    articles.forEach((var article) -> article.setHasPriority(true));
    prioritySet.addAll(articles);
}
```

✅ **Section comments end with ":"** - Comments that introduce a code section should have a colon at the end and apply to multiple lines below
```java
// Toppings:
count = selectableToppingArticles.size();
final var availableArticles = getAvailableArticles();
```

✅ **Line comments for single-line context** - Use trailing comments for context that applies to only one line
```java
createNotificationChannel(context);  // Required for Android O+
vibratorService.vibrate(pattern, -1);  // -1 means no repeat
```

✅ **No redundant comments for self-explanatory function calls** - Don't add comments that just repeat what the function name already says
```java
// Good - no comment needed, function name is clear:
createNotificationChannel(context);
playBeeps(context);
vibrate(context);

// Good - trailing comment adds useful context:
createNotificationChannel(context);  // Required for Android O+

// Bad - comment just repeats function name:
// Create notification channel:
createNotificationChannel(context);
```

✅ **No magic numbers** - Use named constants instead of literal numbers, including time conversion factors
```java
// Good - named constants:
private static final int WEEK_MAX_INDEX = 6;
private static final int SECONDS_PER_MINUTE = 60;
private static final int MILLIS_PER_SECOND = 1000;
long poolSeconds = dailyMinutes * SECONDS_PER_MINUTE * days;
long delayMillis = delaySeconds * MILLIS_PER_SECOND;

// Bad - magic numbers inline:
int max = 6;
long poolSeconds = dailyMinutes * 60 * days;
long delayMillis = delaySeconds * 1000;
```

✅ **Short class descriptions** - Keep class-level Javadoc brief and to the point

✅ **No spaces in casts** - Type casts should be compact
```java
(float)value  // Good
(float) value  // Bad
```

✅ **Common abbreviations are acceptable but else try to avoid them** - Standard abbreviations improve readability
```java
sharedPrefs  // Good - common abbreviation
PREFS_NAME   // Good - well-known abbreviation
btn          // Bad - unclear abbreviation
```

✅ **Use `var` for local variables** - When the type is clear from context
```java
final var context = getApplicationContext();  // Good
final Context context = getApplicationContext();  // Also acceptable when clarity needed
```

✅ **Small lambdas as one-liners** - Simple lambda expressions (1-2 statements) should be on a single line
```java
// Good - single statement:
btnSave.setOnClickListener(v -> saveData());
list.forEach(item -> process(item));

// Good - simple conditional:
launcher.launch("file.txt", uri -> { if (uri != null) handleUri(uri); });

// Keep multi-line for complex lambdas (3+ statements, try-catch, loops, nested structures):
popup.setOnMenuItemClickListener(item ->
{
    switch (item.getItemId())
    {
        case 0: return handleCase0();
        default: return false;
    }
});
```

✅ **Keep statements on single lines** - Don't break up short expressions across multiple lines
```java
// Good - single line for short expressions:
View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
int color = isSelected ? getColor(R.color.selected) : getColor(R.color.unselected);
ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, items);

// Good - multi-line for list-like structures (constructors/calls with many similar parameters):
controlPanelManager = new ControlPanelManager(
    this,
    timeEntryRepository,
    dailyTimePoolRepository,
    preferencesManager,
    findViewById(R.id.btn_start_stop),
    findViewById(R.id.btn_reset)
);

// Bad - breaking short expressions across lines:
View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item, parent, false);
int color = isSelected ?
    getColor(R.color.selected) :
    getColor(R.color.unselected);
```

## Additional Conventions

- Use descriptive variable names
- Keep methods focused on a single responsibility
- Use `final` for variables that won't be reassigned
- Prefer streams and lambdas for collection operations
