import initSqlJs from "https://cdn.jsdelivr.net/npm/sql.js@1.12.0/+esm";

const SQLITE_INTEGER = 1;
const SQLITE_FLOAT = 2;
const SQLITE_TEXT = 3;
const SQLITE_BLOB = 4;
const SQLITE_NULL = 5;

const databases = new Map();
const statements = new Map();
let nextDatabaseId = 1;
let nextStatementId = 1;

const sqlJs = await initSqlJs({
  locateFile: (fileName) => `https://cdn.jsdelivr.net/npm/sql.js@1.12.0/dist/${fileName}`,
});

const postSuccess = (id, data) => self.postMessage({ id, data });

const postError = (id, error) => self.postMessage({
  id,
  error: error instanceof Error ? error.message : String(error),
});

const toSqliteType = (value) => {
  if (value === null || value === undefined) return SQLITE_NULL;
  if (value instanceof Uint8Array) return SQLITE_BLOB;
  if (typeof value === "number") {
    return Number.isInteger(value) ? SQLITE_INTEGER : SQLITE_FLOAT;
  }
  return SQLITE_TEXT;
};

const readAllRows = (statement) => {
  const rows = [];
  let columnTypes = [];

  while (statement.step()) {
    const row = statement.get();
    rows.push(row);
    if (columnTypes.length === 0) {
      columnTypes = row.map(toSqliteType);
    }
  }

  statement.reset();
  return { rows, columnTypes };
};

self.onmessage = async (event) => {
  const { id, data } = event.data;
  const request = data ?? {};

  try {
    switch (request.cmd) {
      case "open": {
        const databaseId = nextDatabaseId++;
        databases.set(databaseId, new sqlJs.Database());
        postSuccess(id, { databaseId });
        break;
      }
      case "prepare": {
        const database = databases.get(request.databaseId);
        if (!database) throw new Error(`Unknown databaseId: ${request.databaseId}`);

        const statement = database.prepare(request.sql);
        const statementId = nextStatementId++;
        statements.set(statementId, statement);

        const parameterNames = typeof statement.getParameterNames === "function"
          ? statement.getParameterNames()
          : [];

        postSuccess(id, {
          statementId,
          parameterCount: parameterNames.length,
          columnNames: statement.getColumnNames(),
        });
        break;
      }
      case "step": {
        const statement = statements.get(request.statementId);
        if (!statement) throw new Error(`Unknown statementId: ${request.statementId}`);

        statement.bind(request.bindings ?? []);
        const result = readAllRows(statement);
        postSuccess(id, result);
        break;
      }
      case "close": {
        if (request.statementId != null) {
          const statement = statements.get(request.statementId);
          statement?.free();
          statements.delete(request.statementId);
          break;
        }

        if (request.databaseId != null) {
          const database = databases.get(request.databaseId);
          database?.close();
          databases.delete(request.databaseId);
        }
        break;
      }
      default: {
        throw new Error(`Unsupported sql.js worker command: ${request.cmd}`);
      }
    }
  } catch (error) {
    postError(id, error);
  }
};
