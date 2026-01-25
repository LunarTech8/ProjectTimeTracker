# Coding Style Guidelines

## Formatting Rules

✅ **No empty lines within functions** - Functions should be compact with no blank lines inside

✅ **Exactly ONE empty line between functions** - Consistent spacing between method declarations

✅ **Line/Section comments end with ":"** - All line/section comments should have a colon at the end
```java
// This is a comment:
```

✅ **No magic numbers** - Use named constants instead of literal numbers
```java
private static final int WEEK_MAX_INDEX = 6;  // Good
int max = 6;  // Bad
```

✅ **Short class descriptions** - Keep class-level Javadoc brief and to the point

✅ **No spaces in casts** - Type casts should be compact
```java
(float)value  // Good
(float) value  // Bad
```

✅ **No abbreviations** - Use full words for clarity
```java
button  // Good
btn     // Bad (except in UI component names from Android conventions)
```

## Additional Conventions

- Use descriptive variable names
- Keep methods focused on a single responsibility
