package android.text;

/**
 * Created by hujingcheng for dial ui of DialtactActivity on 19-6-25.
 */

public class BirdStaticLayout extends StaticLayout {

    private int wordsOneLine=-1;//calculate from first line's end position , so only used for shape of rectangle

    public BirdStaticLayout(CharSequence text) {
        super(text);
    }

    @Override
    public boolean customizeStaticLayout(){
        return true;
    }

    /**
     *  adjust position for dial ui,  bottom lines has priority to be filled up first
     * @return adjusted position
     */
    @Override
    public int[] changePositionBeforeOut(int start,int end,int words){
        int[] positions=new int[2];
        positions[0]=start;
        positions[1]= end;
        if(start==0){
            wordsOneLine=end;
            if(wordsOneLine>0){
                int firstLine=words%wordsOneLine;
                if(firstLine==0 ){
                    firstLine=wordsOneLine;
                }
                positions[1]= firstLine;
            }
        }

        return positions;
    }


}
