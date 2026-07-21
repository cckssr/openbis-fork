import React, { useRef, useEffect } from 'react';
import jspreadsheet from 'jspreadsheet-ce';
import 'jspreadsheet-ce/dist/jspreadsheet.css';
import 'jsuites/dist/jsuites.css';

const DEFAULT_DIMENSIONS = [10, 10];
const DEFAULT_COLUMN_WIDTH = 50;

/**
 * Editable spreadsheet field backed by jspreadsheet community edition (formula support included).
 *
 * Reads and writes the openBIS `Spreadsheet` DTO model — the same format the legacy ELN uses:
 *   { version, headers: string[], data: string[][] (formulas),
 *     values: string[][] (computed), width: number[], style: {"A1": css}, meta: {} }
 *
 * `value` is a Spreadsheet DTO instance (or null/'' for an empty property). On any edit, a fresh
 * plain object is emitted through `onChange`; base64/<DATA> wrapping is done downstream by
 * AbstractEntityPropertyHolder.setSpreadsheetProperty.
 *
 * Uses jspreadsheet directly (not the @jspreadsheet-ce/react wrapper) to support React Strict
 * Mode: the wrapper has no useEffect cleanup, so its guard `!mainReference.current` prevents
 * re-initialisation after the intentional Strict Mode unmount/remount cycle, leaving a blank div.
 */
const SpreadsheetField = ({ value, editable = false, onChange = _v => {} }) => {
  const domRef = useRef(null); // DOM element jspreadsheet attaches to
  const instanceRef = useRef(null); // worksheet instance (instances[0])
  const onChangeRef = useRef(onChange);
  const isInitializingRef = useRef(true);

  // Keep the callback ref current on every render — no stale closures in event handlers
  useEffect(() => {
    onChangeRef.current = onChange;
  });

  // Initialise once per mount; destroy on unmount (Strict Mode safe)
  useEffect(() => {
    if (!domRef.current) {
      return;
    }

    const model = value && typeof value === 'object' ? value : null;
    const data = model?.data ?? [];
    const headers = model?.headers ?? null;
    const width = model?.width ?? null;
    const style = model?.style ?? {};
    const version = model?.version ?? '1';

    const columns = headers
      ? headers.map((title, i) => ({
          title,
          width: width?.[i] ?? DEFAULT_COLUMN_WIDTH,
        }))
      : undefined;

    const handleChange = worksheet => {
      if (isInitializingRef.current) {
        return;
      }
      const ws = worksheet || instanceRef.current;
      if (!ws || !onChangeRef.current) {
        return;
      }

      const headers = ws.getHeaders(true); // string[]
      const rawData = ws.getData(); // CellValue[][]

      // Resolve formula cells to their computed values, mirroring JExcelEditorManager.getOnChange
      const values = rawData.map((row, ri) =>
        row.map((cell, ci) => {
          if (typeof cell === 'string' && cell.startsWith('=')) {
            return ws.getValue(headers[ci] + (ri + 1), true);
          }
          return cell;
        })
      );

      onChangeRef.current({
        version,
        headers,
        data: rawData,
        values,
        width: ws.getWidth(),
        style: ws.getStyle(),
        meta: ws.getMeta(),
      });
    };

    const worksheetOptions = {
      data,
      style,
      minDimensions: DEFAULT_DIMENSIONS,
      defaultColWidth: DEFAULT_COLUMN_WIDTH,
      editable,
      allowInsertRow: editable,
      allowManualInsertRow: editable,
      allowInsertColumn: editable,
      allowManualInsertColumn: editable,
      allowDeleteRow: editable,
      allowDeleteColumn: editable,
      allowRenameColumn: editable,
      allowComments: false,
    };
    if (columns !== undefined) {
      worksheetOptions.columns = columns;
    }

    const spreadsheetOptions = {
      worksheets: [worksheetOptions],
      tabs: false,
      toolbar: toolbar => {
        const remove = new Set(['undo', 'redo', 'save', 'web', 'fullscreen']);
        const itemsCount = toolbar.items.length;
        toolbar.items = toolbar.items
          .filter(
            // The last divisor should be ignored. The border selector too.
            (item, index) => !(item.type === 'divisor' && index < itemsCount - 1 || remove.has(item.content) ||
                item.options && item.options.length > 0 &&
                (item.options[0].startsWith('border_') || item.options[0].startsWith('vertical_')))
          );
        return toolbar;
      }
    };

    if (editable) {
      Object.assign(spreadsheetOptions, {
        onchange: handleChange,
        onafterchanges: handleChange,
        oninsertrow: handleChange,
        oninsertcolumn: handleChange,
        ondeleterow: handleChange,
        ondeletecolumn: handleChange,
        onmoverow: handleChange,
        onmovecolumn: handleChange,
        onchangeheader: handleChange,
        onchangestyle: handleChange,
        onchangemeta: handleChange,
        onresizecolumn: handleChange,
        onresizerow: handleChange,
        onsort: handleChange,
        onpaste: handleChange,
        onundo: handleChange,
        onredo: handleChange,
      });
    }

    const instances = jspreadsheet(domRef.current, spreadsheetOptions);
    instanceRef.current = instances[0];
    isInitializingRef.current = false;

    return () => {
      if (domRef.current) {
        jspreadsheet.destroy(domRef.current, true);
      }
      instanceRef.current = null;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // runs once per mount; the `key` prop in SpreadsheetFieldRenderer forces remount on mode change

  return (
    <div style={{ overflowX: 'auto', width: '100%' }}>
      <div ref={domRef} />
    </div>
  );
};

export default SpreadsheetField;
