package com.quickcommand.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.quickcommand.model.ActionType;
import com.quickcommand.model.Command;
import com.quickcommand.model.CommandConverters;
import com.quickcommand.model.GestureType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CommandDao_Impl implements CommandDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Command> __insertionAdapterOfCommand;

  private final CommandConverters __commandConverters = new CommandConverters();

  private final EntityDeletionOrUpdateAdapter<Command> __deletionAdapterOfCommand;

  private final EntityDeletionOrUpdateAdapter<Command> __updateAdapterOfCommand;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCommandById;

  private final SharedSQLiteStatement __preparedStmtOfSetCommandEnabled;

  public CommandDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCommand = new EntityInsertionAdapter<Command>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `commands` (`id`,`name`,`gestureType`,`customGesturePoints`,`actionType`,`actionParam`,`isEnabled`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Command entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        final String _tmp = __commandConverters.fromGestureType(entity.getGestureType());
        statement.bindString(3, _tmp);
        if (entity.getCustomGesturePoints() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCustomGesturePoints());
        }
        final String _tmp_1 = __commandConverters.fromActionType(entity.getActionType());
        statement.bindString(5, _tmp_1);
        if (entity.getActionParam() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getActionParam());
        }
        final int _tmp_2 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        statement.bindLong(8, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfCommand = new EntityDeletionOrUpdateAdapter<Command>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `commands` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Command entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCommand = new EntityDeletionOrUpdateAdapter<Command>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `commands` SET `id` = ?,`name` = ?,`gestureType` = ?,`customGesturePoints` = ?,`actionType` = ?,`actionParam` = ?,`isEnabled` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Command entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        final String _tmp = __commandConverters.fromGestureType(entity.getGestureType());
        statement.bindString(3, _tmp);
        if (entity.getCustomGesturePoints() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCustomGesturePoints());
        }
        final String _tmp_1 = __commandConverters.fromActionType(entity.getActionType());
        statement.bindString(5, _tmp_1);
        if (entity.getActionParam() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getActionParam());
        }
        final int _tmp_2 = entity.isEnabled() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteCommandById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM commands WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetCommandEnabled = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE commands SET isEnabled = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertCommand(final Command command, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCommand.insertAndReturnId(command);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCommand(final Command command, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCommand.handle(command);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCommand(final Command command, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCommand.handle(command);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCommandById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCommandById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteCommandById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setCommandEnabled(final long id, final boolean enabled,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetCommandEnabled.acquire();
        int _argIndex = 1;
        final int _tmp = enabled ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetCommandEnabled.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Command>> getAllCommands() {
    final String _sql = "SELECT * FROM commands ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"commands"}, new Callable<List<Command>>() {
      @Override
      @NonNull
      public List<Command> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGestureType = CursorUtil.getColumnIndexOrThrow(_cursor, "gestureType");
          final int _cursorIndexOfCustomGesturePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "customGesturePoints");
          final int _cursorIndexOfActionType = CursorUtil.getColumnIndexOrThrow(_cursor, "actionType");
          final int _cursorIndexOfActionParam = CursorUtil.getColumnIndexOrThrow(_cursor, "actionParam");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Command> _result = new ArrayList<Command>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Command _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final GestureType _tmpGestureType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfGestureType);
            _tmpGestureType = __commandConverters.toGestureType(_tmp);
            final String _tmpCustomGesturePoints;
            if (_cursor.isNull(_cursorIndexOfCustomGesturePoints)) {
              _tmpCustomGesturePoints = null;
            } else {
              _tmpCustomGesturePoints = _cursor.getString(_cursorIndexOfCustomGesturePoints);
            }
            final ActionType _tmpActionType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfActionType);
            _tmpActionType = __commandConverters.toActionType(_tmp_1);
            final String _tmpActionParam;
            if (_cursor.isNull(_cursorIndexOfActionParam)) {
              _tmpActionParam = null;
            } else {
              _tmpActionParam = _cursor.getString(_cursorIndexOfActionParam);
            }
            final boolean _tmpIsEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Command(_tmpId,_tmpName,_tmpGestureType,_tmpCustomGesturePoints,_tmpActionType,_tmpActionParam,_tmpIsEnabled,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Command>> getEnabledCommands() {
    final String _sql = "SELECT * FROM commands WHERE isEnabled = 1 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"commands"}, new Callable<List<Command>>() {
      @Override
      @NonNull
      public List<Command> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGestureType = CursorUtil.getColumnIndexOrThrow(_cursor, "gestureType");
          final int _cursorIndexOfCustomGesturePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "customGesturePoints");
          final int _cursorIndexOfActionType = CursorUtil.getColumnIndexOrThrow(_cursor, "actionType");
          final int _cursorIndexOfActionParam = CursorUtil.getColumnIndexOrThrow(_cursor, "actionParam");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Command> _result = new ArrayList<Command>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Command _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final GestureType _tmpGestureType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfGestureType);
            _tmpGestureType = __commandConverters.toGestureType(_tmp);
            final String _tmpCustomGesturePoints;
            if (_cursor.isNull(_cursorIndexOfCustomGesturePoints)) {
              _tmpCustomGesturePoints = null;
            } else {
              _tmpCustomGesturePoints = _cursor.getString(_cursorIndexOfCustomGesturePoints);
            }
            final ActionType _tmpActionType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfActionType);
            _tmpActionType = __commandConverters.toActionType(_tmp_1);
            final String _tmpActionParam;
            if (_cursor.isNull(_cursorIndexOfActionParam)) {
              _tmpActionParam = null;
            } else {
              _tmpActionParam = _cursor.getString(_cursorIndexOfActionParam);
            }
            final boolean _tmpIsEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Command(_tmpId,_tmpName,_tmpGestureType,_tmpCustomGesturePoints,_tmpActionType,_tmpActionParam,_tmpIsEnabled,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCommandById(final long id, final Continuation<? super Command> $completion) {
    final String _sql = "SELECT * FROM commands WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Command>() {
      @Override
      @Nullable
      public Command call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGestureType = CursorUtil.getColumnIndexOrThrow(_cursor, "gestureType");
          final int _cursorIndexOfCustomGesturePoints = CursorUtil.getColumnIndexOrThrow(_cursor, "customGesturePoints");
          final int _cursorIndexOfActionType = CursorUtil.getColumnIndexOrThrow(_cursor, "actionType");
          final int _cursorIndexOfActionParam = CursorUtil.getColumnIndexOrThrow(_cursor, "actionParam");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final Command _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final GestureType _tmpGestureType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfGestureType);
            _tmpGestureType = __commandConverters.toGestureType(_tmp);
            final String _tmpCustomGesturePoints;
            if (_cursor.isNull(_cursorIndexOfCustomGesturePoints)) {
              _tmpCustomGesturePoints = null;
            } else {
              _tmpCustomGesturePoints = _cursor.getString(_cursorIndexOfCustomGesturePoints);
            }
            final ActionType _tmpActionType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfActionType);
            _tmpActionType = __commandConverters.toActionType(_tmp_1);
            final String _tmpActionParam;
            if (_cursor.isNull(_cursorIndexOfActionParam)) {
              _tmpActionParam = null;
            } else {
              _tmpActionParam = _cursor.getString(_cursorIndexOfActionParam);
            }
            final boolean _tmpIsEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new Command(_tmpId,_tmpName,_tmpGestureType,_tmpCustomGesturePoints,_tmpActionType,_tmpActionParam,_tmpIsEnabled,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
