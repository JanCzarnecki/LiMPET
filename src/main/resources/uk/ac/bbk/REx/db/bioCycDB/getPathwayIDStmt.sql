SELECT
    pat.WID
FROM
    DataSet d,
    Pathway pat
WHERE
    d.Name=?
    && d.WID=pat.DataSetWID
    && pat.Name=?