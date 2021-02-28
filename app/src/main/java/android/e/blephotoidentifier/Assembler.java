package android.e.blephotoidentifier;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;

public class Assembler {
    private static HashMap<String,byte[]> gatherMap = new HashMap<>();
    private static HashMap<String,Integer> expectMap = new HashMap<>();

    public static byte[] gather(String address,byte[] data){
        if (!gatherMap.containsKey(address)){
            if (data[0] == (byte)1){
                Log.e("START:", Long.toString(System.currentTimeMillis()));
                if (data[1] == (byte)1){
                    int null_ind = data.length;
                    for (int k = 2; k < data.length; k++){
                        if (data[k] == (byte)0){
                            null_ind = k;
                            break;
                        }
                    }
                    return Arrays.copyOfRange(data,2,data.length);
                }
                gatherMap.put(address, Arrays.copyOfRange(data,2,data.length));
                expectMap.put(address,2);
            } else {
                expectMap.put(address,1);
            }
        } else {
            int expected = expectMap.get(address);
            if (data[0] != (byte)expected){
                return null;
            } else {
                expected++;
                expectMap.put(address,expected);
                byte[] temp = gatherMap.get(address);
                byte[] conjoin = new byte[temp.length + data.length - 2];
                for (int i = 0; i < temp.length; i++){
                    conjoin[i] = temp[i];
                }
                for (int j = 0; j < data.length - 2; j++){
                    conjoin[temp.length+j] = data[j+2];
                }
                gatherMap.put(address,conjoin);
                if (data[1] == (byte)1){
                    Log.e("END:", Long.toString(System.currentTimeMillis()));
                    gatherMap.remove(address);
                    expectMap.remove(address);
                    int null_ind = conjoin.length;
                    for (int k = 2; k < data.length; k++){
                        if (data[k] == (byte)0){
                            null_ind = k;
                            break;
                        }
                    }
                    conjoin = Arrays.copyOfRange(conjoin,0,conjoin.length-245+null_ind);
                    return conjoin;
                }
            }
        }
        return null;
    }

    public static void clear(){
        gatherMap.clear();
        expectMap.clear();
    }
}
