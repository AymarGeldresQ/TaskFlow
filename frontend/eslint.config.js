// @ts-check
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('@angular-eslint/eslint-plugin');
const angularTemplate = require('@angular-eslint/eslint-plugin-template');
const templateParser = require('@angular-eslint/template-parser');

const FEATURES = ['auth', 'teams', 'projects', 'board', 'tasks', 'labels'];

/**
 * Builds cross-feature boundary patterns: files inside features/X must not import from features/Y (X !== Y).
 */
function crossFeaturePatterns(featureName) {
  return FEATURES.filter((f) => f !== featureName).map((other) => ({
    group: [`**/features/${other}/**`, `../../../features/${other}/**`, `../../features/${other}/**`, `../features/${other}/**`],
    message: `Do not import from features/${other} inside features/${featureName}. Use shared core services or the router instead.`,
  }));
}

module.exports = tseslint.config(
  eslint.configs.recommended,
  // Scope strict type-checked rules to TS files only (HTML uses a different parser)
  ...tseslint.configs.strictTypeChecked.map((config) => ({
    ...config,
    files: config.files ?? ['**/*.ts'],
  })),
  {
    files: ['**/*.ts'],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.app.json', './tsconfig.spec.json'],
      },
    },
    plugins: {
      '@angular-eslint': angular,
    },
    rules: {
      // TypeScript strict
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/explicit-function-return-type': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/prefer-readonly': 'error',
      '@typescript-eslint/no-floating-promises': 'error',
      // Angular Validators.required/email/etc. are static methods — unbound-method is a false positive here
      '@typescript-eslint/unbound-method': ['error', { ignoreStatic: true }],

      // Angular
      '@angular-eslint/component-class-suffix': 'error',
      '@angular-eslint/directive-class-suffix': 'error',
      '@angular-eslint/no-input-rename': 'error',
      '@angular-eslint/no-output-rename': 'error',
      '@angular-eslint/use-lifecycle-interface': 'error',
      '@angular-eslint/component-selector': [
        'error',
        { type: 'element', prefix: 'tf', style: 'kebab-case' },
      ],

      // General
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      eqeqeq: ['error', 'always'],
    },
  },
  // Cross-feature boundary enforcement per feature
  ...FEATURES.map((featureName) => ({
    files: [`src/app/features/${featureName}/**/*.ts`],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: crossFeaturePatterns(featureName),
        },
      ],
    },
  })),
  {
    files: ['**/*.html'],
    languageOptions: {
      parser: templateParser,
    },
    plugins: {
      '@angular-eslint/template': angularTemplate,
    },
    rules: {
      '@angular-eslint/template/no-negated-async': 'error',
      '@angular-eslint/template/eqeqeq': 'error',
    },
  },
  {
    ignores: ['dist/', 'node_modules/', '**/*.spec.ts'],
  },
);
